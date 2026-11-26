<!-- AI-assisted. -->

# CVNStack infrastructure

This program creates one Compute Engine VM and runs the repository Compose stack
on it: PostgreSQL, Redis, and the service image. CDK for Terraform was archived
in December 2025, so this uses Pulumi, the maintained CDK-style tool for GCP.

Project `cvnstack` denies external IP addresses on VMs
(`constraints/compute.vmExternalIpAccess`). The VM stays on a private subnet.
Cloud NAT gives it outbound access for Docker. A regional network load balancer
holds the public address and forwards TCP `80` and `8080` to the VM. Port `80`
is redirected to the app on `8080` on the host. The free site name is an
[sslip.io](https://sslip.io) hostname that resolves to that address:

`http://waitlist-management-service-<suffix>.<ip-with-dashes>.sslip.io`

The suffix is stored in `Pulumi.dev.yaml` as `cvnstack:siteSuffix`. PostgreSQL
`5432` and Redis `6379` stay closed. SSH is only through Identity-Aware Proxy.
The VM is an `e2-medium` Debian 12 instance in `us-central1-a`.

## Create the instance

From `infra/`, with the Cloud CLI already logged in as a project owner. Install the
Pulumi CLI from <https://www.pulumi.com/docs/install/> when `pulumi` is not on
your PATH. The dev stack has no secrets; the empty passphrase matches
`Pulumi.dev.yaml`.

```bash
npm install
export PULUMI_CONFIG_PASSPHRASE=""
pulumi login --local
pulumi stack select dev
pulumi preview
pulumi up
```

`pulumi up` creates the network, firewall, service account, address, and VM.
The first boot installs Docker. It does not copy the application yet.

## Start the containers

```bash
./deploy-app.sh
pulumi stack output siteUrl
pulumi stack output curlCommand
curl -sS "$(pulumi stack output serviceUrl)"
```

`deploy-app.sh` copies this repository to `/opt/cvnstack` and starts
`cvnstack.service`, which runs `docker compose up -d --build`. The image build
on an `e2-medium` can take several minutes. Change `cvnstack:machineType` in
`Pulumi.dev.yaml` if the build runs out of memory, then run `pulumi up` again.

Remove the instance and its network with `pulumi destroy`.
