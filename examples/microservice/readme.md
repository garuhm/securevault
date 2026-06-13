This example service code consumes auth-service's `user-events` Kafka topic to maintain:
1. A denormalized `ownerUsername` column on entities that need it for
   filtering/sorting (combo option 3).
2. A Redis point-lookup cache (`user-cache:{userId}` -> "username|email")
   for display-only fields not worth denormalizing (combo option 2).

Both are eventually-consistent copies of auth-service's data — never
write back to either as if they were authoritative.

Bootstrap/reconciliation on cold start: if this service starts after users
already exist in Keycloak, query auth-service's internal user-list endpoint
(or replay the Kafka topic from the beginning with a fresh consumer group)
to backfill before relying on steady-state event consumption.