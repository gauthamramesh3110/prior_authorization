# Ollama in Docker

The Compose service runs Ollama on CPU and stores models in the `ollama_data`
named volume. Models persist when the container is stopped or recreated.
Only one model is loaded at a time, with one parallel request, to limit memory use.

From the repository root, start Ollama:

```powershell
docker compose up -d ollama
```

For a new model volume, install the models used by the payer:

```powershell
docker compose exec ollama ollama pull phi4-mini
docker compose exec ollama ollama pull nomic-embed-text:v1.5
```

The payer runs on the host and uses `http://localhost:11434`, matching its current
`application.yaml`. The port is published only on the local computer. Quit the
native Ollama application before starting the container if it uses the same port.
The application does not automatically pull models, so complete model setup
before starting the payer or ingesting policies.

Check availability and logs:

```powershell
docker compose ps ollama
docker compose exec ollama ollama list
docker compose logs --tail 50 ollama
```

Stop Ollama without stopping PostgreSQL:

```powershell
docker compose stop ollama
```

If the payer is later containerized on the same Compose network, its Ollama base
URL should be `http://ollama:11434`.
