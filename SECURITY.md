# Baremaps Security Model

This document describes the security assumptions and roles for Baremaps. It clarifies who is trusted and their responsibilities.

## Roles

* **Administrator**: Administrates Baremaps, managing the system, configurations, tasks, and data through the CLI or other administrative interfaces.
* **User**: Uses Baremaps through limited, controlled interfaces such as the web interface or API.

## Trust Model

* **Trusted**:

    * Administrators

* **Untrusted**:

    * Users

## Security Assumptions

* Administrators are fully trusted and responsible for the security of the system and application.
* Configuration files and data sources are managed exclusively by trusted administrators.

## Security Boundaries

* Baremaps relies entirely on trusted administrators to manage files and data.
* Risks such as file path traversal are low because only administrators have access to critical configuration and data inputs.

### Example: Partial Path Traversal Vulnerability in Zip Files

Administrator-provided files are trusted. For instance, if an administrator uses a zip file with a path traversal vulnerability, Baremaps does not systematically sanitize the content, as the administrator is expected to ensure the integrity of the files.

User-provided files are not trusted. For instance, if a user uploads a file through an HTTP endpoint, Baremaps must systematically sanitize and validate the content to block partial path traversal attacks.

## Reporting Security Issues

Report vulnerabilities privately through [GitHub security advisories](https://github.com/baremaps/baremaps/security/advisories/new). Please do not open public issues for security problems. The Baremaps maintainers will acknowledge the report and coordinate a fix and disclosure with you.
