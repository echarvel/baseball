# Python ML Service — Team Recommendation Engine

A separate Flask microservice (port 5001) that recommends similar baseball players using K-Nearest Neighbors.

## What It Does

Given a "seed" player (or physical traits), finds the most similar players based on 5 features:
- **Birth year** (z-scored)
- **Height** (z-scored)
- **Weight** (z-scored)
- **Bats** (R/L/N → 1/-1/0)
- **Throws** (R/L/N → 1/-1/0)

This is **not clustering** — KNN doesn't group data upfront. It's a similarity search computed on demand. Given one player's point in 5D feature space, it finds the N closest points by Euclidean distance.

Z-scoring normalizes features to the same scale — otherwise weight (120-250 lbs) would dominate over bats (-1/0/1).

## Endpoints

### `POST /team/generate` — find similar players

By seed player:
```bash
curl -H "Content-type: application/json" \
  -d '{"seed_id":"aaronha01","team_size":10}' \
  http://localhost:5001/team/generate
```

By physical features (missing features default to mean):
```bash
curl -H "Content-type: application/json" \
  -d '{"features":{"birth_year":1970,"height":70,"weight":120,"bats":"R","throws":"L"},"team_size":10}' \
  http://localhost:5001/team/generate
```

### `POST /team/feedback` — exclude bad recommendations

```bash
curl -H "Content-type: application/json" \
  -d '{"seed_id":"aaronha01","member_id":"maurero01","feedback":-1}' \
  http://localhost:5001/team/feedback
```

`-1` = bad (adds to exclusion list), `1` = good (no-op currently). Excluded players won't appear in future recommendations for that seed.

### `POST /llm/generate` and `POST /llm/feedback` — stubbed out, not implemented

## How the Model Works

1. **Training** (`train.ipynb` Jupyter notebook):
   - Loads `player.csv`, normalizes features (z-score), encodes handedness
   - Fits `NearestNeighbors(n_neighbors=25)` from scikit-learn
   - Exports `team_model.joblib` (trained model) and `features_db.csv` (processed data)

2. **Runtime** (`server.py`):
   - Loads pre-built model and feature DB
   - Uses `nn_model.kneighbors()` to find closest players
   - Built-in chaos monkey: 1% timeout error, 1% 6-second delay

## Tech Stack

- Flask (web framework)
- pandas (data manipulation)
- scikit-learn (KNN model)
- pydantic (request/response validation)
- joblib (model serialization)
- Poetry (dependency management)

## Running It

```bash
# Locally (port 5001)
cd player-service-backend/player-service-model/a4a_model
python3 server.py

# With Docker (change port in Dockerfile or use -p 5001:5001)
cd player-service-backend/player-service-model
docker build -t a4a_model .
docker run -d -p 5001:5000 a4a_model
```

Note: macOS AirPlay Receiver uses port 5000 by default — we run on 5001 instead.

## Connection to Java Backend

The Java app calls this service via `RestTemplate` (Spring's HTTP client):

```
Java Endpoints                          Python Flask Endpoints
──────────────                          ──────────────────────
GET  /v1/team/generate?seedId=X    →    POST /team/generate
POST /v1/team/feedback             →    POST /team/feedback
```

Files:
- `TeamController.java` — REST endpoints
- `TeamService.java` — calls Python service via RestTemplate

The ML service URL is currently hardcoded in `TeamService.java`. The proper approach would be to add it to `application.yml` and use `@ConfigurationProperties`, similar to how `ChatClientConfiguration` handles the Ollama URL.

## Key Files

```
player-service-model/
  a4a_model/
    server.py          <-- Flask app with endpoints
    model.py           <-- Empty train() stub
    train.ipynb        <-- Jupyter notebook that trained the model
    team_model.joblib  <-- Pre-trained KNN model
    features_db.csv    <-- Processed player features
    player.csv         <-- Raw player data
  Dockerfile           <-- Container build
  pyproject.toml       <-- Poetry dependencies
```

## Pydantic Models (Request/Response Validation)

```python
class TeamGenerateInput(BaseModel):
    seed_id: Optional[str] = None       # lookup by player ID
    features: Optional[Features] = None  # OR by physical traits
    team_size: int

class TeamGenerateOutput(BaseModel):
    seed_id: Optional[str]
    prediction_id: str                   # unique ID for this recommendation
    team_size: int
    member_ids: list[str]                # recommended player IDs

class TeamFeedbackInput(BaseModel):
    seed_id: str
    member_id: str
    feedback: Literal[-1, 1]             # -1 = bad, 1 = good

class Features(BaseModel):
    birth_year: Optional[float] = None
    height: Optional[float] = None       # inches
    weight: Optional[float] = None       # pounds
    bats: Optional[Literal["L", "R", "N"]] = None
    throws: Optional[Literal["L", "R", "N"]] = None
```
