<!-- AI-assisted. -->

# CVNStack GCP Deployment Guide

This document records the shared Google Cloud Platform (GCP) project, networking, managed store onboarding, Cloud Run deployment commands, and mentor demo walkthrough for CVNStack. No secrets or passwords are stored in this file.

## 1. Project, region, APIs, IAM, and VPC (issue #42)

### Project and region

| Setting | Value |
| --- | --- |
| Project ID | `cvnstack` |
| Project Name | `CVNStack` |
| Project Number | `806111529624` |
| Region | `us-central1` |
| Zone | `us-central1-a` |
| Billing | Enabled |

### Enabled APIs

The following GCP APIs are enabled on `cvnstack` for networking, managed stores, container registry, secret management, and serverless hosting:

- `compute.googleapis.com` (Compute Engine and VPC networking)
- `servicenetworking.googleapis.com` (Private Services Access for managed stores)
- `vpcaccess.googleapis.com` (Serverless VPC Access)
- `sqladmin.googleapis.com` (Cloud SQL Admin)
- `redis.googleapis.com` (Memorystore for Redis)
- `secretmanager.googleapis.com` (Secret Manager)
- `artifactregistry.googleapis.com` (Artifact Registry)
- `run.googleapis.com` (Cloud Run)
- `iam.googleapis.com` and `iap.googleapis.com` (IAM and Identity-Aware Proxy)

Reproducible command to enable the Iteration 2 service APIs:

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  redis.googleapis.com \
  secretmanager.googleapis.com \
  vpcaccess.googleapis.com \
  artifactregistry.googleapis.com \
  --project=cvnstack
```

### VPC and Serverless VPC Access connector

| Resource | Name / Value |
| --- | --- |
| VPC network | `cvnstack` (custom subnet mode) |
| Primary subnet | `cvnstack` (`us-central1`, `10.10.0.0/24`) |
| Serverless VPC Access connector | `cvnstack-connector` |
| Full connector resource | `projects/cvnstack/locations/us-central1/connectors/cvnstack-connector` |
| Connector IP CIDR range | `10.8.0.0/28` |
| Connector sizing | `e2-micro`, min 2 / max 3 instances (200-300 Mbps) |
| Verified state | `READY` (2026-11-25) |

Reproducible command used to create and verify the connector:

```bash
gcloud compute networks vpc-access connectors create cvnstack-connector \
  --project=cvnstack \
  --region=us-central1 \
  --network=cvnstack \
  --range=10.8.0.0/28 \
  --min-instances=2 \
  --max-instances=3 \
  --machine-type=e2-micro

gcloud compute networks vpc-access connectors describe cvnstack-connector \
  --project=cvnstack \
  --region=us-central1
```

### IAM access

| Member | Role | Purpose |
| --- | --- | --- |
| `zk2343@columbia.edu` | `roles/owner` | Project owner and billing/IAM administrator |
| `yk3193@columbia.edu` | `roles/editor` | GCP onboarding and deployment |
| `dw3258@columbia.edu` | `roles/editor` | Teammate verification and integration |
| Mentor (pending owner grant) | Pending (`roles/viewer` or demo role) | Second Iteration Demo access |

Because only `roles/owner` (or `roles/resourcemanager.projectIamAdmin`) can edit project IAM bindings, the project owner grants mentor access once the mentor's Google account email is confirmed:

```bash
gcloud projects add-iam-policy-binding cvnstack \
  --member="user:<MENTOR_GOOGLE_EMAIL>" \
  --role="roles/viewer"
```

## 2. Cloud SQL for PostgreSQL (issue #43)

Pending issue #43.

## 3. Memorystore for Redis (issue #44)

### Instance details

| Setting | Value |
| --- | --- |
| Instance name | `cvnstack-redis` |
| Full resource | `projects/cvnstack/locations/us-central1/instances/cvnstack-redis` |
| Region | `us-central1` |
| Tier and size | `BASIC`, `1 GB` |
| Redis version | `REDIS_7_2` |
| VPC network | `projects/cvnstack/global/networks/cvnstack` (`DIRECT_PEERING`) |
| Private host (`REDIS_HOST`) | `10.63.253.187` |
| Port (`REDIS_PORT`) | `6379` |
| TLS / AUTH | Disabled on private VPC (`REDIS_SSL=false`, no `REDIS_PASSWORD`) |
| Verified state | `READY` (2026-11-26) |

### Application configuration keys

The service reads the same Redis configuration keys already defined in `src/main/resources/application.properties` (no code change required):

| Environment variable | Spring property | Value for `cvnstack-redis` |
| --- | --- | --- |
| `REDIS_HOST` | `spring.data.redis.host` | `10.63.253.187` |
| `REDIS_PORT` | `spring.data.redis.port` | `6379` |
| `REDIS_SSL` | `spring.data.redis.ssl.enabled` | `false` |

### Reproducible commands

```bash
gcloud redis instances create cvnstack-redis \
  --project=cvnstack \
  --region=us-central1 \
  --tier=basic \
  --size=1 \
  --redis-version=redis_7_2 \
  --network=projects/cvnstack/global/networks/cvnstack \
  --connect-mode=DIRECT_PEERING

gcloud redis instances describe cvnstack-redis \
  --project=cvnstack \
  --region=us-central1 \
  --format="table(name,host,port,state,redisVersion,tier,memorySizeGb,authorizedNetwork)"
```

## 4. Cloud Run deployment (issue #45)

Pending issue #45.

## 5. Live walkthrough for the mentor (issue #46)

Pending issue #46.
