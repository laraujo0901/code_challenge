# Implementation Decisions

Record of some decisions taken during service implementation.

1. Currency query param is not required at controller level, to permit a fine-grained exception handling, overriding default exception thrown by Spring Boot when a required param is not present.

2. H2 in-memory database was chosen for simplicity and ease of use. But, thanks to Spring Data JPA, switching to an real database, like PostgreSQL is easy.

3. Take all rates of exchange from Treasury API and persist in database on application startup was a initial approach, but API have rate limit and rejected connection from the service. So, I changed to a incremental strategy, persisting successful query results to optimize conversions to saved currency in future requests.

4. Retry with exponential backoff to add resilience when connection to API is unstable.

5. Slice was chosen over Page, because the latter requires count all records in database, while Slice gets the specified records in page size plus 1. So, you know you have more records to fetch without count the entire table.