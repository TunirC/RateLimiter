# Designing Rate Limiters - Low level design

## Objective
Design and implement a Rate Limiter that restricts the number of requests a user/service can make in a given time window.

## Requirements
- Limit number of requests per user per time window. 
- Thread-safe. 
- Extendable to global limits or IP-based limits. 
- Should support different strategies
  - Fixed Window 
  - Sliding Window 
  - Token Bucket 
  - Leaky Bucket