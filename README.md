# Spotify OAuth Integration with Spring Boot

This Java Spring Boot application demonstrates Spotify API integration using OAuth 2.0 with the PKCE flow.

## Overview

SpotifyOAuthIntegration is a Java Spring Boot application that showcases how to securely integrate with the Spotify API using OAuth 2.0 and the PKCE (Proof Key for Code Exchange) flow. This project provides a robust and secure method for user authentication, token management (including refresh), and accessing Spotify's Web API, specifically focusing on retrieving user's top tracks.

## Features

*   **OAuth 2.0 Authentication with PKCE:** Implements PKCE for enhanced security during the authorization process, preventing authorization code interception attacks.
*   **Authorization Code Exchange:** Exchanges the authorization code received from Spotify for an access token and refresh token.
*   **Token Management:** Includes support for automatic token refreshing to ensure uninterrupted access to the Spotify API.
*   **User Top Tracks Retrieval:** Implements functionality to retrieve the authenticated user's top played tracks.
*   **Error Handling:** Includes custom exception handling for Spotify API errors and JSON parsing issues.
*   **Spring Boot Framework:** Built with a clean, modular, and scalable Spring Boot structure.
*   **Centralized WebClient Configuration:** Uses a dedicated configuration class for managing the WebClient used for Spotify API requests.

## Technologies Used

*   Java (17 or higher)
*   Spring Boot (3.2.11 or higher)
*   Spring Web
*   Spring Security (with OAuth 2.0 Resource Server configuration)
*   Spring WebFlux (for WebClient)
*   Jackson (for JSON processing)
*   Maven (for dependency management)

## Getting Started

### Prerequisites

*   Java 17 or higher installed on your machine.
*   Maven installed (optional if using Maven Wrapper)
*   A Spotify Developer account:
    *   Create a Spotify app to obtain the `Client ID`, `Client Secret`, and configure the `Redirect URI`.
    *   Ensure your Spotify app has the necessary scopes enabled, such as `user-read-private`, `user-read-email`, `playlist-read-private`, `playlist-modify-public`, and `user-top-read`

### Configuration

1.  Clone the repository:

    ```bash
git clone https://github.com/karolgalvao/SpotifyOAuthIntegration.git

2.  Navigate to the project directory:

    ```bash
    cd SpotifyOAuthIntegration
    ```

3.  Create an `application.properties` (or `application.yml`) file in the `src/main/resources` directory.

4.  Add the following properties to your `application.properties` file, replacing the placeholders with your actual values:

    ```properties
    spotify.client_id=<your_client_id>
    spotify.client_secret=<your_client_secret>
    spotify.redirect_uri=<your_redirect_uri>
    ```

### Running the Application

1.  Build the project using Maven:

    ```bash
    ./mvnw spring-boot:run # Using Maven Wrapper (recommended)
    mvn spring-boot:run # If you have Maven installed globally
    ```

2.  Access the application in your browser or using a tool like Postman.

## Endpoints

*   `/auth/spotify`: Endpoint to initiate the Spotify authorization flow.
*   `/auth/callback`: Callback endpoint that Spotify redirects to after authorization.
*   `/user/profile`: Endpoint to retrieve the authenticated user's profile. Requires authentication with a valid access token.
*   `/playlists`: Endpoint to retrieve the authenticated user's playlists. Requires authentication with a valid access token and the appropriate scopes (e.g., `playlist-read-private`).
*   `/user/top-tracks`: Endpoint to retrieve the user's top tracks (requires authentication with the `user-top-read` scope).

## Further Development

*   Implement pagination for top tracks.
*   Add support for other Spotify API endpoints.
*   Improve error handling and logging.
*   Add unit and integration tests.

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.
