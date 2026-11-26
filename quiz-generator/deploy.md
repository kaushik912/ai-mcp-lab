# Deploy to Google Cloud Run

The server holds no secrets (BYOK), so no env vars or Secret Manager are needed.
Not tested from the dev sandbox (no GCP access); verify on your machine.

## One-time setup
1. Create a Google Cloud project and attach a billing account (card required, even for free-tier use).
2. Install the `gcloud` CLI, then:
   ```
   gcloud auth login
   gcloud config set project <PROJECT_ID>
   gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
   ```
3. Billing > Budgets & alerts: add a small budget alert (e.g. $5) as a safety net.

## Deploy (and redeploy)
```
cd quiz-generator
gcloud run deploy quiz-generator \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --max-instances 2 \
  --memory 512Mi \
  --cpu-boost
```
Cloud Build builds the `Dockerfile`, pushes the image, and prints an `https://...run.app` URL. Open it on your phone.

| Flag | Why |
|------|-----|
| `--allow-unauthenticated` | Public page; fine because there is no server secret |
| `--max-instances 2` | Caps hosting cost |
| `--memory 512Mi` | Use `1Gi` if the container is OOM-killed |
| `--cpu-boost` | Faster Spring Boot cold start |
| `--region us-central1` | Free-tier-eligible region; check current Cloud Run pricing page |

## Notes
- Scales to zero: first request after idle takes ~5-15s (cold start). Open the page a minute before practising.
- Default request timeout is 5 minutes, plenty for a quiz call.
- Logs: `gcloud run services logs read quiz-generator --region us-central1`. The app never logs the API key.
- Delete when done: `gcloud run services delete quiz-generator --region us-central1`.
- Optional: connect the GitHub repo in the Cloud Run console for deploy-on-push.
