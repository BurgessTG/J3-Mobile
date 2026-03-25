FROM golang:1.25-alpine AS builder

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o /bridge ./cmd/bridge

FROM alpine:3.21

COPY --from=builder /bridge /bridge
EXPOSE 8181
CMD ["/bridge"]
