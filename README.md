# Library Microservices

Two Spring Boot microservices for a small library system.

## Services

- `book-service`: manages an in-memory book catalogue.
- `loan-service`: manages in-memory loans and validates books through `book-service`.

## API

### book-service

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/books` | Lists books. |
| `GET` | `/books/{id}` | Returns a book or `404 Not Found`. |
| `POST` | `/books` | Creates a book and returns `201 Created`. |

### loan-service

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/loans` | Lists loans. |
| `GET` | `/loans/{id}` | Returns a loan or `404 Not Found`. |
| `POST` | `/loans` | Creates a loan after validating the book. |

## Run with Docker Compose

```bash
docker compose up --build -d
docker compose logs -f
docker compose down
```

- Books API: `http://localhost:8081`
- Loans API: `http://localhost:8082`

`loan-service` uses `BOOK_SERVICE_URL=http://book-service:8080` to reach `book-service` inside the Compose network.

## Manual API checks

```bash
curl -i http://localhost:8081/books
curl -i http://localhost:8081/books/1

curl -i -X POST http://localhost:8081/books \
  -H 'Content-Type: application/json' \
  -d '{"title":"Dune","author":"Frank Herbert"}'

curl -i http://localhost:8082/loans

curl -i -X POST http://localhost:8082/loans \
  -H 'Content-Type: application/json' \
  -d '{"bookId":1,"borrowerName":"Ana"}'

curl -i -X POST http://localhost:8082/loans \
  -H 'Content-Type: application/json' \
  -d '{"bookId":999,"borrowerName":"Ana"}'
```

The final request must return `404 Not Found`.

## Kubernetes

```bash
kubectl apply -f kubernetes/
kubectl get deployments,pods,services

kubectl port-forward service/book-service 8081:8080
kubectl port-forward service/loan-service 8082:8080
```

The manifests are in the `kubernetes/` directory. `book-service` has three replicas and `loan-service` resolves it through the `book-service` Kubernetes Service.

## Tests

```bash
(cd book-service && mvn test)
(cd loan-service && mvn test)
docker compose config
kubectl apply --dry-run=server -f kubernetes/
```
