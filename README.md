# Steps to reproduce

1. Start the Docker services. This also pulls the latest images and deletes orphans.

    ```bash
    make up
    ```

2. Run the test.

    ```bash
    make test
    ```

3. Wait a bit until `actual-output.csv` appears.

4. Compare results.

    ```bash
    make compare
    ```
5. Shutdown the Docker services.

    ```bash
    make down
    ```