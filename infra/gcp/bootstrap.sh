#!/usr/bin/env bash
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID, for example money-hunter-123456}"
: "${BILLING_ACCOUNT_ID:?Set BILLING_ACCOUNT_ID from gcloud billing accounts list}"

REGION="${REGION:-asia-northeast3}"
CREATE_PROJECT="${CREATE_PROJECT:-false}"
LINK_BILLING="${LINK_BILLING:-false}"
GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-kimjuneon/money-hunter}"
ARTIFACT_REPOSITORY="${ARTIFACT_REPOSITORY:-money-hunter}"
SQL_INSTANCE="${SQL_INSTANCE:-money-hunter-postgres}"
DB_VERSION="${DB_VERSION:-POSTGRES_16}"
DB_EDITION="${DB_EDITION:-enterprise}"
DB_TIER="${DB_TIER:-db-f1-micro}"
DB_USER="${DB_USER:-money_hunter_app}"
PROD_DB_NAME="${PROD_DB_NAME:-money_hunter}"
REVIEW_DB_NAME="${REVIEW_DB_NAME:-money_hunter_review}"
DB_PASSWORD_SECRET="${DB_PASSWORD_SECRET:-money-hunter-db-password}"
RUNTIME_SERVICE_ACCOUNT="${RUNTIME_SERVICE_ACCOUNT:-money-hunter-runner}"
DEPLOYER_SERVICE_ACCOUNT="${DEPLOYER_SERVICE_ACCOUNT:-money-hunter-github-deployer}"
WIF_POOL="${WIF_POOL:-github-pool}"
WIF_PROVIDER="${WIF_PROVIDER:-github-provider}"
REVIEW_SERVICE="${REVIEW_SERVICE:-money-hunter-review}"
PROD_SERVICE="${PROD_SERVICE:-money-hunter-prod}"

if ! gcloud projects describe "$PROJECT_ID" >/dev/null 2>&1; then
	if [ "$CREATE_PROJECT" != "true" ]; then
		echo "Project ${PROJECT_ID} does not exist. Set CREATE_PROJECT=true to create it." >&2
		exit 1
	fi
	gcloud projects create "$PROJECT_ID" --name="Money Hunter"
fi

gcloud config set project "$PROJECT_ID"

if [ "$(gcloud billing projects describe "$PROJECT_ID" --format="value(billingEnabled)" 2>/dev/null || true)" != "True" ]; then
	if [ "$LINK_BILLING" != "true" ]; then
		echo "Billing is not enabled for ${PROJECT_ID}. Link billing first or set LINK_BILLING=true." >&2
		exit 1
	fi
	gcloud billing projects link "$PROJECT_ID" --billing-account "$BILLING_ACCOUNT_ID"
fi

gcloud services enable \
	cloudresourcemanager.googleapis.com \
	iam.googleapis.com \
	run.googleapis.com \
	artifactregistry.googleapis.com \
	cloudbuild.googleapis.com \
	sqladmin.googleapis.com \
	secretmanager.googleapis.com \
	iamcredentials.googleapis.com \
	sts.googleapis.com

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format="value(projectNumber)")"
RUNTIME_SA="${RUNTIME_SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com"
DEPLOYER_SA="${DEPLOYER_SERVICE_ACCOUNT}@${PROJECT_ID}.iam.gserviceaccount.com"
CLOUD_BUILD_SA="${PROJECT_NUMBER}@cloudbuild.gserviceaccount.com"

if ! gcloud artifacts repositories describe "$ARTIFACT_REPOSITORY" --location "$REGION" >/dev/null 2>&1; then
	gcloud artifacts repositories create "$ARTIFACT_REPOSITORY" \
		--repository-format docker \
		--location "$REGION" \
		--description "Money Hunter application images"
fi

if ! gcloud sql instances describe "$SQL_INSTANCE" >/dev/null 2>&1; then
	gcloud sql instances create "$SQL_INSTANCE" \
		--database-version "$DB_VERSION" \
		--edition "$DB_EDITION" \
		--tier "$DB_TIER" \
		--region "$REGION" \
		--storage-size 10 \
		--availability-type zonal
fi

for database in "$PROD_DB_NAME" "$REVIEW_DB_NAME"; do
	if ! gcloud sql databases describe "$database" --instance "$SQL_INSTANCE" >/dev/null 2>&1; then
		gcloud sql databases create "$database" --instance "$SQL_INSTANCE"
	fi
done

if ! gcloud secrets describe "$DB_PASSWORD_SECRET" >/dev/null 2>&1; then
	openssl rand -base64 32 | gcloud secrets create "$DB_PASSWORD_SECRET" --data-file=-
fi

DB_PASSWORD="$(gcloud secrets versions access latest --secret "$DB_PASSWORD_SECRET")"
if ! gcloud sql users list --instance "$SQL_INSTANCE" --format="value(name)" | grep -Fx "$DB_USER" >/dev/null; then
	gcloud sql users create "$DB_USER" --instance "$SQL_INSTANCE" --password "$DB_PASSWORD"
else
	gcloud sql users set-password "$DB_USER" --instance "$SQL_INSTANCE" --password "$DB_PASSWORD"
fi

for service_account in "$RUNTIME_SERVICE_ACCOUNT" "$DEPLOYER_SERVICE_ACCOUNT"; do
	if ! gcloud iam service-accounts describe "${service_account}@${PROJECT_ID}.iam.gserviceaccount.com" >/dev/null 2>&1; then
		gcloud iam service-accounts create "$service_account"
	fi
done

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
	--member "serviceAccount:${RUNTIME_SA}" \
	--role roles/cloudsql.client
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
	--member "serviceAccount:${RUNTIME_SA}" \
	--role roles/secretmanager.secretAccessor

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
	--member "serviceAccount:${DEPLOYER_SA}" \
	--role roles/run.admin
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
	--member "serviceAccount:${DEPLOYER_SA}" \
	--role roles/artifactregistry.writer
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
	--member "serviceAccount:${DEPLOYER_SA}" \
	--role roles/cloudbuild.builds.editor
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
	--member "serviceAccount:${DEPLOYER_SA}" \
	--role roles/iam.serviceAccountUser
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
	--member "serviceAccount:${CLOUD_BUILD_SA}" \
	--role roles/artifactregistry.writer

if ! gcloud iam workload-identity-pools describe "$WIF_POOL" --location global >/dev/null 2>&1; then
	gcloud iam workload-identity-pools create "$WIF_POOL" \
		--location global \
		--display-name "GitHub Actions"
fi

if ! gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER" \
		--location global \
		--workload-identity-pool "$WIF_POOL" >/dev/null 2>&1; then
	gcloud iam workload-identity-pools providers create-oidc "$WIF_PROVIDER" \
		--location global \
		--workload-identity-pool "$WIF_POOL" \
		--display-name "GitHub ${GITHUB_REPOSITORY}" \
		--issuer-uri "https://token.actions.githubusercontent.com" \
		--attribute-mapping "google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
		--attribute-condition "attribute.repository=='${GITHUB_REPOSITORY}'"
fi

PROVIDER_RESOURCE="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WIF_POOL}/providers/${WIF_PROVIDER}"
PRINCIPAL_SET="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WIF_POOL}/attribute.repository/${GITHUB_REPOSITORY}"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER_SA" \
	--project "$PROJECT_ID" \
	--member "$PRINCIPAL_SET" \
	--role roles/iam.workloadIdentityUser

if command -v gh >/dev/null 2>&1 && [ "${SET_GITHUB_VARIABLES:-false}" = "true" ]; then
	gh variable set GCP_PROJECT_ID --repo "$GITHUB_REPOSITORY" --body "$PROJECT_ID"
	gh variable set GCP_REGION --repo "$GITHUB_REPOSITORY" --body "$REGION"
	gh variable set GCP_ARTIFACT_REPOSITORY --repo "$GITHUB_REPOSITORY" --body "$ARTIFACT_REPOSITORY"
	gh variable set GCP_CLOUD_SQL_INSTANCE --repo "$GITHUB_REPOSITORY" --body "${PROJECT_ID}:${REGION}:${SQL_INSTANCE}"
	gh variable set GCP_DB_USER --repo "$GITHUB_REPOSITORY" --body "$DB_USER"
	gh variable set GCP_DB_PASSWORD_SECRET --repo "$GITHUB_REPOSITORY" --body "$DB_PASSWORD_SECRET"
	gh variable set GCP_PROD_DB_NAME --repo "$GITHUB_REPOSITORY" --body "$PROD_DB_NAME"
	gh variable set GCP_REVIEW_DB_NAME --repo "$GITHUB_REPOSITORY" --body "$REVIEW_DB_NAME"
	gh variable set GCP_RUNTIME_SERVICE_ACCOUNT --repo "$GITHUB_REPOSITORY" --body "$RUNTIME_SA"
	gh variable set GCP_DEPLOYER_SERVICE_ACCOUNT --repo "$GITHUB_REPOSITORY" --body "$DEPLOYER_SA"
	gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --repo "$GITHUB_REPOSITORY" --body "$PROVIDER_RESOURCE"
	gh variable set GCP_REVIEW_SERVICE --repo "$GITHUB_REPOSITORY" --body "$REVIEW_SERVICE"
	gh variable set GCP_PROD_SERVICE --repo "$GITHUB_REPOSITORY" --body "$PROD_SERVICE"
fi

cat <<EOF

GCP bootstrap complete.

Set these GitHub repository variables if SET_GITHUB_VARIABLES=true was not used:
GCP_PROJECT_ID=${PROJECT_ID}
GCP_REGION=${REGION}
GCP_ARTIFACT_REPOSITORY=${ARTIFACT_REPOSITORY}
GCP_CLOUD_SQL_INSTANCE=${PROJECT_ID}:${REGION}:${SQL_INSTANCE}
GCP_DB_USER=${DB_USER}
GCP_DB_PASSWORD_SECRET=${DB_PASSWORD_SECRET}
GCP_PROD_DB_NAME=${PROD_DB_NAME}
GCP_REVIEW_DB_NAME=${REVIEW_DB_NAME}
GCP_RUNTIME_SERVICE_ACCOUNT=${RUNTIME_SA}
GCP_DEPLOYER_SERVICE_ACCOUNT=${DEPLOYER_SA}
GCP_WORKLOAD_IDENTITY_PROVIDER=${PROVIDER_RESOURCE}
GCP_REVIEW_SERVICE=${REVIEW_SERVICE}
GCP_PROD_SERVICE=${PROD_SERVICE}

Review URL after deployment:
gcloud run services describe ${REVIEW_SERVICE} --project ${PROJECT_ID} --region ${REGION} --format='value(status.url)'

Production URL after deployment:
gcloud run services describe ${PROD_SERVICE} --project ${PROJECT_ID} --region ${REGION} --format='value(status.url)'
EOF
