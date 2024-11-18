# SpotifyOAuthIntegration
A Java Spring Boot application demonstrating Spotify API integration using OAuth 2.0 with PKCE flow.

## Overview
SpotifyOAuthIntegration is a Java Spring Boot application that demonstrates how to integrate with the Spotify API using OAuth 2.0 with the PKCE (Proof Key for Code Exchange) flow. This project showcases secure user authentication and token management to access Spotify's Web API.

---

## Features
- **OAuth 2.0 Authentication**:
  - Implements PKCE for secure authorization.
  - Exchanges authorization code for an access token.
- **Callback Handling**:
  - Processes authorization responses securely.
- **Token Management**:
  - Includes support for token refreshing.
- **Java Spring Boot Framework**:
  - Clean, modular, and scalable structure.

---

## Technologies Used
- **Java** (17)
- **Spring Boot** (3.2.11)
  - Spring Web
  - Spring Security
- **WebClient** for HTTP requests
- **Maven** for dependency management

---

## Getting Started

### Prerequisites
1. **Java 17** or higher installed on your machine.
2. **Maven** installed (optional if using Maven Wrapper).
3. A Spotify Developer account:
   - [Create a Spotify app](https://developer.spotify.com/dashboard/applications) to get the **Client ID**, **Client Secret**, and configure the **Redirect URI**.

---
