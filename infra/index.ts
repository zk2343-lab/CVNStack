// AI-assisted.
// One Compute Engine VM with a free sslip.io site name for curl access.

import * as crypto from "crypto";
import * as fs from "fs";
import * as path from "path";
import * as pulumi from "@pulumi/pulumi";
import * as gcp from "@pulumi/gcp";

const project = gcp.config.project;
if (!project) {
  throw new Error("Set gcp:project in the stack config.");
}

const config = new pulumi.Config();
const region = gcp.config.region ?? "us-central1";
const zone = gcp.config.zone ?? "us-central1-a";
const machineType = config.get("machineType") ?? "e2-medium";
const siteSuffix = config.get("siteSuffix")
  ?? crypto.randomBytes(4).toString("hex");
const startupScript = fs.readFileSync(path.join(__dirname, "startup.sh"), "utf8");

function enableApi(name: string, service: string): gcp.projects.Service {
  return new gcp.projects.Service(name, {
    project,
    service,
    disableOnDestroy: false,
  });
}

const computeApi = enableApi("compute-api", "compute.googleapis.com");
const iamApi = enableApi("iam-api", "iam.googleapis.com");
const iapApi = enableApi("iap-api", "iap.googleapis.com");

const network = new gcp.compute.Network("cvnstack", {
  name: "cvnstack",
  autoCreateSubnetworks: false,
}, { dependsOn: [computeApi] });

const subnet = new gcp.compute.Subnetwork("cvnstack", {
  name: "cvnstack",
  network: network.id,
  region,
  ipCidrRange: "10.10.0.0/24",
});

const httpFirewall = new gcp.compute.Firewall("cvnstack-http", {
  name: "cvnstack-http",
  network: network.id,
  direction: "INGRESS",
  sourceRanges: ["0.0.0.0/0"],
  targetTags: ["cvnstack-http"],
  allows: [{ protocol: "tcp", ports: ["80", "8080"] }],
});

const sshFirewall = new gcp.compute.Firewall("cvnstack-iap-ssh", {
  name: "cvnstack-iap-ssh",
  network: network.id,
  direction: "INGRESS",
  sourceRanges: ["35.235.240.0/20"],
  targetTags: ["cvnstack-ssh"],
  allows: [{ protocol: "tcp", ports: ["22"] }],
});

const address = new gcp.compute.Address("cvnstack", {
  name: "cvnstack",
  region,
  addressType: "EXTERNAL",
  networkTier: "PREMIUM",
});

const router = new gcp.compute.Router("cvnstack", {
  name: "cvnstack",
  region,
  network: network.id,
});

const nat = new gcp.compute.RouterNat("cvnstack", {
  name: "cvnstack",
  router: router.name,
  region,
  natIpAllocateOption: "AUTO_ONLY",
  sourceSubnetworkIpRangesToNat: "LIST_OF_SUBNETWORKS",
  subnetworks: [{
    name: subnet.id,
    sourceIpRangesToNats: ["ALL_IP_RANGES"],
  }],
});

const serviceAccount = new gcp.serviceaccount.Account("cvnstack-vm", {
  accountId: "cvnstack-vm",
  displayName: "CVNStack compute instance",
}, { dependsOn: [iamApi] });

const serviceAccountMember = pulumi.interpolate`serviceAccount:${serviceAccount.email}`;

new gcp.projects.IAMMember("cvnstack-vm-logs", {
  project,
  role: "roles/logging.logWriter",
  member: serviceAccountMember,
});

new gcp.projects.IAMMember("cvnstack-vm-metrics", {
  project,
  role: "roles/monitoring.metricWriter",
  member: serviceAccountMember,
});

const instance = new gcp.compute.Instance("cvnstack", {
  name: "cvnstack",
  machineType,
  zone,
  tags: ["cvnstack-http", "cvnstack-ssh"],
  allowStoppingForUpdate: true,
  bootDisk: {
    initializeParams: {
      image: "debian-cloud/debian-12",
      size: 40,
      type: "pd-balanced",
    },
  },
  networkInterfaces: [{
    subnetwork: subnet.id,
  }],
  metadataStartupScript: startupScript,
  serviceAccount: {
    email: serviceAccount.email,
    scopes: [
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring.write",
    ],
  },
}, { dependsOn: [computeApi, iapApi, httpFirewall, sshFirewall, nat] });

const instanceGroup = new gcp.compute.InstanceGroup("cvnstack", {
  name: "cvnstack",
  zone,
  network: network.id,
  instances: [instance.selfLink],
  namedPorts: [{ name: "http", port: 8080 }],
});

const healthCheck = new gcp.compute.RegionHealthCheck("cvnstack", {
  name: "cvnstack",
  region,
  httpHealthCheck: {
    port: 8080,
    requestPath: "/health",
  },
});

const backend = new gcp.compute.RegionBackendService("cvnstack", {
  name: "cvnstack",
  region,
  protocol: "TCP",
  loadBalancingScheme: "EXTERNAL",
  healthChecks: healthCheck.id,
  backends: [{
    group: instanceGroup.id,
    balancingMode: "CONNECTION",
  }],
});

new gcp.compute.ForwardingRule("cvnstack", {
  name: "cvnstack",
  region,
  loadBalancingScheme: "EXTERNAL",
  ipAddress: address.address,
  ipProtocol: "TCP",
  portRange: "8080",
  networkTier: "PREMIUM",
  backendService: backend.id,
});

new gcp.compute.ForwardingRule("cvnstack-http", {
  name: "cvnstack-http",
  region,
  loadBalancingScheme: "EXTERNAL",
  ipAddress: address.address,
  ipProtocol: "TCP",
  portRange: "80",
  networkTier: "PREMIUM",
  backendService: backend.id,
});

const siteHost = address.address.apply((ip) => {
  const dashed = ip.replace(/\./g, "-");
  return `waitlist-management-service-${siteSuffix}.${dashed}.sslip.io`;
});

export const instanceName = instance.name;
export const instanceZone = instance.zone;
export const externalIp = address.address;
export const siteName = siteHost;
export const siteUrl = pulumi.interpolate`http://${siteHost}`;
export const serviceUrl = pulumi.interpolate`http://${siteHost}/health`;
export const curlCommand = pulumi.interpolate`curl -sS http://${siteHost}/health`;
export const sshCommand = pulumi.interpolate`gcloud compute ssh ${instance.name} --project ${project} --zone ${zone} --tunnel-through-iap`;
