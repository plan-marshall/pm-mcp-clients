# The Section Cut of the Specification Move

One row per section (heading with its anchor) of every specification document this move touches, with exactly
one home. `pm-mcp-clients` is this repository, `plan-marshall-mcp` is the daemon repository (its plain plan
`doc/specification-move-plan.md` reads this table as its task 2), and `stays` means the section remains in
`plan-marshall-documentation`.

Source: `plan-marshall-documentation` at `79ec3a6e` (PR #190 and #191 merged), `pm-mcp-core` at `d506e818`,
`pm-mcp-clients` at `42fb9b1d`. The population is the `Specified in` column of the Modules table of
`doc/specification/module-structure.adoc` for the modules of this repository (`pm-api`, `pm-exec`, `pm-relay`,
`pm-operator`, `pm-mock-runtime`, `pm-web-app`, `pm-web-e2e`), read against the reviewer's reading of the
doc-review session (`doc-review-2026-11-17-001`).

## Method

- A section is split at its heading, never inside a paragraph. A row carries the heading text as written and
  its anchor: the explicit `[#id]` / `[[id]]` where the source has one, otherwise the id Asciidoctor generates
  from the title (the rule `trace.py` implements: markup stripped, then lower case, then every character
  outside space, word, `.` and `-` removed, then each run of space, `.` and `-` as one `_`, then trimmed and
  prefixed with `_`).
- Block anchors that are not headings (a paragraph carrying `[[id]]` or `[#id]`) travel with the section they
  sit in and are listed in the appendix; they are not rows.
- The count check (file tools only, no program): `grep -Ec '^=+ '` per source document (`-E`, because `+` is
  literal in the basic regular expressions of `grep`) against the rows of that document in the table. Totals at
  the end.
- A document title is a heading and has a row. Where a family is split, the family's `Overview`, `Traceability`,
  `Status` and the daemon's `Modules` rows stay with the family index of the repository that holds the family's
  daemon part: `cli-and-security` (the listeners, the pairing) and `runtime-model` (the runtime itself) keep them
  in `plan-marshall-mcp`. `harness-integration` moves whole, so its index moves with it. Each repository's index
  lists and describes only the parts it holds (new index rows, not moved sections), and links the other one.
- Two hypotheses of the spec were settled against the source before the table was written, both recorded in
  the progress record: `harness-integration` belongs here (see the note under its rows), and the requirement
  modules of `plan-marshall-documentation` carry no `Implementation:`/`Verified by:`/`Documentation:` blocks
  (`doc/Traceability.adoc` here is authored from this repository's code and tests, as `pm-mcp-core` does).

## The Table

This is a plain Markdown file, and the table is a GitHub-flavoured Markdown table (the rows carry no trailing
pipe; that is optional in GFM).

| # | Document | Anchor | Section | Home |
|---|---|---|---|---|
|1 |`cli-and-security.adoc` |`_cli_coordination_security_specification` |(title) CLI, Coordination & Security Specification |plan-marshall-mcp
|2 |`cli-and-security.adoc` |`_overview` |Overview |plan-marshall-mcp
|3 |`cli-and-security.adoc` |`_traceability` |Traceability |plan-marshall-mcp
|4 |`cli-and-security.adoc` |`_status_in_progress` |Status: IN PROGRESS |plan-marshall-mcp
|5 |`cli-and-security.adoc` |`_modules` |Modules |plan-marshall-mcp
|6 |`cli-and-security.adoc` |`_parts` |Parts |plan-marshall-mcp
|7 |`cli-and-security/01-cli-and-installer.adoc` |`_cli_coordination_security_specification_cli_binaries_harness_installer_entry` |(title) CLI Binaries, Harness Installer & Entry |pm-mcp-clients
|8 |`cli-and-security/01-cli-and-installer.adoc` |`cli-binary` |Command-Line Binaries (`pm-mcp`, `pm-operator`) & Installation Layout |pm-mcp-clients
|9 |`cli-and-security/01-cli-and-installer.adoc` |`installation-layout` |Installation Layout |pm-mcp-clients
|10 |`cli-and-security/01-cli-and-installer.adoc` |`command-tree` |Command Tree |pm-mcp-clients
|11 |`cli-and-security/01-cli-and-installer.adoc` |`cli-daemon-access` |Runtime Access & On-Demand Start |pm-mcp-clients
|12 |`cli-and-security/01-cli-and-installer.adoc` |`client-environment` |What the Terminal Client Sends |pm-mcp-clients
|13 |`cli-and-security/01-cli-and-installer.adoc` |`cli-implementation` |CLI Implementation |pm-mcp-clients
|14 |`cli-and-security/01-cli-and-installer.adoc` |`_process_coordination` |Process Coordination |pm-mcp-clients
|15 |`cli-and-security/01-cli-and-installer.adoc` |`two-channels` |Operator Choices in the Web App and the Terminal |pm-mcp-clients
|16 |`cli-and-security/01-cli-and-installer.adoc` |`start` |The Terminal Client as the Entry |pm-mcp-clients
|17 |`cli-and-security/01-cli-and-installer.adoc` |`auto` |Automatic Onboarding |pm-mcp-clients
|18 |`cli-and-security/01-cli-and-installer.adoc` |`harness-installer` |Harness Bundle Installer (`pm-operator harness`) |pm-mcp-clients
|19 |`cli-and-security/01-cli-and-installer.adoc` |`_embedded_classpath_bundle_layout` |Embedded Classpath Bundle Layout |pm-mcp-clients
|20 |`cli-and-security/01-cli-and-installer.adoc` |`_path_traversal_integrity_guards_during_bundle_extraction` |Path Traversal & Integrity Guards During Bundle Extraction |pm-mcp-clients
|21 |`cli-and-security/01-cli-and-installer.adoc` |`_pm_operator_harness_cli_subcommands_install_update_remove_status` |`pm-operator harness` CLI Subcommands (`install`, `update`, `remove`, `status`) |pm-mcp-clients
|22 |`cli-and-security/01-cli-and-installer.adoc` |`harness-operator-gate` |Operator Gate for Security-Weakening Changes |pm-mcp-clients
|23 |`cli-and-security/01-cli-and-installer.adoc` |`bundle-drift-notice` |Bundle Drift Notice |pm-mcp-clients
|24 |`cli-and-security/02-operator-channel.adoc` |`_cli_coordination_security_specification_operator_credential_acts_operator_channel` |(title) Operator Credential Acts & Operator Channel |pm-mcp-clients
|25 |`cli-and-security/02-operator-channel.adoc` |`operator-credential-cli` |Operator Credential Acts & Runtime Secret Redaction |pm-mcp-clients
|26 |`cli-and-security/02-operator-channel.adoc` |`operator-channel-isolation` |Operator Channel Isolation & Operator Acts (`pm-operator`) |pm-mcp-clients
|27 |`cli-and-security/03-web-server.adoc` |`_cli_coordination_security_specification_web_access_pairing` |(title) Web Access & Pairing |plan-marshall-mcp
|28 |`cli-and-security/03-web-server.adoc` |`web-server` |Web Access & Pairing (`pm-operator web`) |plan-marshall-mcp
|29 |`cli-and-security/03-web-server.adoc` |`two-listeners` |Loopback and LAN Listeners Side by Side |plan-marshall-mcp
|30 |`cli-and-security/03-web-server.adoc` |`web-server-lifecycle` |Lifecycle (`pm-operator web enable\|disable`) |plan-marshall-mcp
|31 |`cli-and-security/03-web-server.adoc` |`web-listener` |Listener Exposure |plan-marshall-mcp
|32 |`cli-and-security/03-web-server.adoc` |`web-security-headers` |Request Validation & Security Headers |plan-marshall-mcp
|33 |`cli-and-security/03-web-server.adoc` |`web-proxy` |API Access & Device Secret |plan-marshall-mcp
|34 |`cli-and-security/03-web-server.adoc` |`web-credentials` |Credentials in the Web App |plan-marshall-mcp
|35 |`cli-and-security/03-web-server.adoc` |`web-pairing` |Pairing Protocol |plan-marshall-mcp
|36 |`cli-and-security/03-web-server.adoc` |`web-devices` |Paired Devices |plan-marshall-mcp
|37 |`cli-and-security/03-web-server.adoc` |`web-operator-channel` |Every Paired Device Is an Operator Channel |plan-marshall-mcp
|38 |`cli-and-security/03-web-server.adoc` |`web-auth-seam` |Authentication Seam |plan-marshall-mcp
|39 |`cli-and-security/03-web-server.adoc` |`web-app` |Web App (`pm-web-app`) |pm-mcp-clients
|40 |`cli-and-security/03-web-server.adoc` |`web-threats` |Threat Notes |plan-marshall-mcp
|41 |`runtime-model.adoc` |`_runtime_interaction_model_specification` |(title) Runtime & Interaction Model Specification |plan-marshall-mcp
|42 |`runtime-model.adoc` |`_overview` |Overview |plan-marshall-mcp
|43 |`runtime-model.adoc` |`_traceability` |Traceability |plan-marshall-mcp
|44 |`runtime-model.adoc` |`_status_in_progress` |Status: IN PROGRESS |plan-marshall-mcp
|45 |`runtime-model.adoc` |`_modules` |Modules |plan-marshall-mcp
|46 |`runtime-model.adoc` |`_parts` |Parts |plan-marshall-mcp
|47 |`runtime-model/01-processes-and-transport.adoc` |`_runtime_interaction_model_specification_processes_transport_local_trust` |(title) Processes, Transport & Local Trust |plan-marshall-mcp
|48 |`runtime-model/01-processes-and-transport.adoc` |`_terminology` |Terminology |plan-marshall-mcp
|49 |`runtime-model/01-processes-and-transport.adoc` |`_process_topology` |Process Topology |plan-marshall-mcp
|50 |`runtime-model/01-processes-and-transport.adoc` |`_runtime` |Runtime |plan-marshall-mcp
|51 |`runtime-model/01-processes-and-transport.adoc` |`_responsibilities` |Responsibilities |plan-marshall-mcp
|52 |`runtime-model/01-processes-and-transport.adoc` |`_singleton_discovery` |Singleton & Discovery |plan-marshall-mcp
|53 |`runtime-model/01-processes-and-transport.adoc` |`_transport_endpoint` |Transport Endpoint |plan-marshall-mcp
|54 |`runtime-model/01-processes-and-transport.adoc` |`runtime-token` |Local Trust: Runtime Token |plan-marshall-mcp
|55 |`runtime-model/01-processes-and-transport.adoc` |`job-token` |Local Trust: Job Token |plan-marshall-mcp
|56 |`runtime-model/01-processes-and-transport.adoc` |`local-api` |Local API (`/api/v1`) |plan-marshall-mcp
|57 |`runtime-model/01-processes-and-transport.adoc` |`_start_modes` |Start Modes |plan-marshall-mcp
|58 |`runtime-model/01-processes-and-transport.adoc` |`runtime-environment` |Environment |plan-marshall-mcp
|59 |`runtime-model/01-processes-and-transport.adoc` |`_resources_of_the_operators_clients` |Resources of the Operator's Clients |plan-marshall-mcp
|60 |`runtime-model/01-processes-and-transport.adoc` |`event-streams` |Events per Scope |plan-marshall-mcp
|61 |`runtime-model/01-processes-and-transport.adoc` |`chat-routes` |The Chat's Routes |plan-marshall-mcp
|62 |`runtime-model/01-processes-and-transport.adoc` |`transcriptions` |Transcriptions |plan-marshall-mcp
|63 |`runtime-model/01-processes-and-transport.adoc` |`process-resource` |The Plan's Process as a Structure |plan-marshall-mcp
|64 |`runtime-model/01-processes-and-transport.adoc` |`process-events` |Events of the Process |plan-marshall-mcp
|65 |`runtime-model/01-processes-and-transport.adoc` |`plan-changes` |The Plan's Changes |plan-marshall-mcp
|66 |`runtime-model/01-processes-and-transport.adoc` |`needs-operator` |What Needs the Operator, at a Glance |plan-marshall-mcp
|67 |`runtime-model/01-processes-and-transport.adoc` |`needs-operator-list` |Everything That Waits, as One List |plan-marshall-mcp
|68 |`runtime-model/01-processes-and-transport.adoc` |`overview-resource` |The Overview Resource and Its Events |plan-marshall-mcp
|69 |`runtime-model/01-processes-and-transport.adoc` |`find-work` |Finding Work by Name |plan-marshall-mcp
|70 |`runtime-model/01-processes-and-transport.adoc` |`project-explorer` |The Projects of the Machine |plan-marshall-mcp
|71 |`runtime-model/01-processes-and-transport.adoc` |`epic-resource` |The Epic as a Resource |plan-marshall-mcp
|72 |`runtime-model/01-processes-and-transport.adoc` |`ended-work` |Ended Work |plan-marshall-mcp
|73 |`runtime-model/01-processes-and-transport.adoc` |`reports` |Reports to the Operator |plan-marshall-mcp
|74 |`runtime-model/03-relay.adoc` |`_runtime_interaction_model_specification_relay` |(title) Relay |pm-mcp-clients
|75 |`runtime-model/03-relay.adoc` |`_relay_pm_mcp_serve_job` |Relay (`pm-mcp serve --job`) |pm-mcp-clients
|76 |`runtime-model/03-relay.adoc` |`_contract` |Contract |pm-mcp-clients
|77 |`runtime-model/03-relay.adoc` |`_harness_protocol_forms` |Harness Protocol Forms |pm-mcp-clients
|78 |`runtime-model/03-relay.adoc` |`_connection_metadata` |Connection Metadata |pm-mcp-clients
|79 |`runtime-model/03-relay.adoc` |`_generations_at_the_relay` |Generations at the Relay |pm-mcp-clients
|80 |`runtime-model/03-relay.adoc` |`relay-rules` |Relay Rules |pm-mcp-clients
|81 |`runtime-model/03-relay.adoc` |`_request_and_response_flow` |Request and Response Flow |pm-mcp-clients
|82 |`runtime-model/03-relay.adoc` |`_normal_call` |Normal Call |pm-mcp-clients
|83 |`runtime-model/03-relay.adoc` |`_messages_from_runtime_to_harness` |Messages from Runtime to Harness |pm-mcp-clients
|84 |`runtime-model/03-relay.adoc` |`_cancellation` |Cancellation |pm-mcp-clients
|85 |`runtime-model/05-mock-runtime.adoc` |`_runtime_interaction_model_specification_mock_runtime` |(title) Mock Runtime |pm-mcp-clients
|86 |`runtime-model/05-mock-runtime.adoc` |`mock-runtime` |Mock Runtime (`pm-mock-runtime`) |pm-mcp-clients
|87 |`runtime-model/05-mock-runtime.adoc` |`mock-endpoints` |Endpoints & Start |pm-mcp-clients
|88 |`runtime-model/05-mock-runtime.adoc` |`scenario-files` |Scenario Files |pm-mcp-clients
|89 |`runtime-model/05-mock-runtime.adoc` |`mock-timing` |Realistic and Deterministic Timing |pm-mcp-clients
|90 |`runtime-model/05-mock-runtime.adoc` |`scenario-coverage` |Scenarios per Flow |pm-mcp-clients
|91 |`runtime-model/05-mock-runtime.adoc` |`scenario-drift` |Drift Against the Real Runtime |pm-mcp-clients
|92 |`runtime-model/06-event-catalogue.adoc` |`_runtime_interaction_model_specification_event_catalogue` |(title) Event Catalogue |pm-mcp-clients
|93 |`runtime-model/06-event-catalogue.adoc` |`event-catalogue` |Event Catalogue |pm-mcp-clients
|94 |`runtime-model/06-event-catalogue.adoc` |`_stream_control` |Stream Control |pm-mcp-clients
|95 |`runtime-model/06-event-catalogue.adoc` |`_runtime_system_cleanup` |Runtime, System & Cleanup |pm-mcp-clients
|96 |`runtime-model/06-event-catalogue.adoc` |`_overview` |Overview |pm-mcp-clients
|97 |`runtime-model/06-event-catalogue.adoc` |`_states_process_decisions` |States, Process & Decisions |pm-mcp-clients
|98 |`runtime-model/06-event-catalogue.adoc` |`chat-events` |Chat |pm-mcp-clients
|99 |`runtime-model/06-event-catalogue.adoc` |`_reports_activity` |Reports & Activity |pm-mcp-clients
|100 |`harness-integration.adoc` |`_harness_integration_bundle_manifest_specification` |(title) Harness Integration & Bundle Manifest Specification |pm-mcp-clients
|101 |`harness-integration.adoc` |`_overview` |Overview |pm-mcp-clients
|102 |`harness-integration.adoc` |`_traceability` |Traceability |pm-mcp-clients
|103 |`harness-integration.adoc` |`_status_in_progress` |Status: IN PROGRESS |pm-mcp-clients
|104 |`harness-integration.adoc` |`_modules` |Modules |pm-mcp-clients
|105 |`harness-integration.adoc` |`_parts` |Parts |pm-mcp-clients
|106 |`harness-integration/01-harness-detection.adoc` |`_harness_integration_bundle_manifest_specification_harness_environment_detection` |(title) Harness Environment Detection |pm-mcp-clients
|107 |`harness-integration/01-harness-detection.adoc` |`harness-environment-detection` |Harness Environment Detection |pm-mcp-clients
|108 |`harness-integration/01-harness-detection.adoc` |`harness-integration-table` |Harness Integration Table (Normative) |pm-mcp-clients
|109 |`harness-integration/01-harness-detection.adoc` |`harness-identity-probes` |Harness Identity Probes (Normative) |pm-mcp-clients
|110 |`harness-integration/02-bundle-manifest.adoc` |`_harness_integration_bundle_manifest_specification_bundle_manifest` |(title) Bundle Manifest |pm-mcp-clients
|111 |`harness-integration/02-bundle-manifest.adoc` |`_embedded_harness_bundle_bundle_manifest_json_schema` |Embedded Harness Bundle `bundle-manifest.json` Schema |pm-mcp-clients
|112 |`harness-integration/02-bundle-manifest.adoc` |`bundle-content` |Bundle Content per Harness |plan-marshall-mcp
|113 |`harness-integration/02-bundle-manifest.adoc` |`_normative_bundle_manifest_json_json_schema_draft_2020_12` |Normative `bundle-manifest.json` JSON Schema (Draft 2020-12) |pm-mcp-clients
|114 |`harness-integration/02-bundle-manifest.adoc` |`_concrete_bundle_manifest_json_example_claude` |Concrete `bundle-manifest.json` Example (`claude`) |pm-mcp-clients
|115 |`harness-integration/02-bundle-manifest.adoc` |`_ownership_digest_verification_on_install_update_remove` |Ownership & Digest Verification on Install/Update/Remove |pm-mcp-clients
|116 |`mcp-tools.adoc` |`_mcp_tool_resource_surface_specification` |(title) MCP Tool & Resource Surface Specification |plan-marshall-mcp
|117 |`mcp-tools.adoc` |`_overview` |Overview |plan-marshall-mcp
|118 |`mcp-tools.adoc` |`_traceability` |Traceability |plan-marshall-mcp
|119 |`mcp-tools.adoc` |`_status_in_progress` |Status: IN PROGRESS |plan-marshall-mcp
|120 |`mcp-tools.adoc` |`_modules` |Modules |plan-marshall-mcp
|121 |`mcp-tools.adoc` |`_parts` |Parts |plan-marshall-mcp
|122 |`mcp-tools/04-errors-and-testing.adoc` |`_mcp_tool_resource_surface_specification_errors_failure_states_protocol_testing` |(title) Errors, Failure States & Protocol Testing |plan-marshall-mcp
|123 |`mcp-tools/04-errors-and-testing.adoc` |`_protocol_errors_tool_execution_errors_iserror_true_domain_workflow_failure_states_iserror_false` |Protocol Errors, Tool Execution Errors (`isError: true`) & Domain Workflow Failure States (`isError: false`) |plan-marshall-mcp
|124 |`mcp-tools/04-errors-and-testing.adoc` |`_normative_classification_matrix` |Normative Classification Matrix |plan-marshall-mcp
|125 |`mcp-tools/04-errors-and-testing.adoc` |`_rejected_transition_representation` |Rejected Transition Representation (`isError: false`) |plan-marshall-mcp
|126 |`mcp-tools/04-errors-and-testing.adoc` |`_closed_rejection_reasons_failure_classes` |Closed Rejection Reasons & Failure Classes |plan-marshall-mcp
|127 |`mcp-tools/04-errors-and-testing.adoc` |`_sanitized_tool_execution_error_envelope` |Sanitized Tool Execution Error Envelope (`isError: true`) |plan-marshall-mcp
|128 |`mcp-tools/04-errors-and-testing.adoc` |`_automated_mcp_protocol_stdio_testing_with_mcpassured_quarkus_mcp_server_test` |Automated MCP Protocol & STDIO Testing with `McpAssured` (`quarkus-mcp-server-test`) |plan-marshall-mcp
|129 |`mcp-tools/04-errors-and-testing.adoc` |`_end_to_end_stdio_integration_testing_mcpstdiotestclient_in_it_java` |End-to-End STDIO Integration Testing (`McpStdioTestClient` in `*IT.java`) |plan-marshall-mcp
|130 |`mcp-tools/04-errors-and-testing.adoc` |`_normative_mcpassured_verification_requirements` |Normative `McpAssured` Verification Requirements |plan-marshall-mcp
|131 |`module-structure.adoc` |`_module_structure_specification` |(title) Module Structure Specification |stays
|132 |`module-structure.adoc` |`_overview` |Overview |stays
|133 |`module-structure.adoc` |`_traceability` |Traceability |stays
|134 |`module-structure.adoc` |`_status_in_progress` |Status: IN PROGRESS |stays
|135 |`module-structure.adoc` |`repositories` |Repositories |stays
|136 |`module-structure.adoc` |`model-facing-content` |Model-Facing Content |stays
|137 |`module-structure.adoc` |`artifact-flow` |Artifact Flow |stays
|138 |`module-structure.adoc` |`module-tree` |Module Tree |stays
|139 |`module-structure.adoc` |`modules` |Modules |stays
|140 |`module-structure.adoc` |`packages` |Packages |stays
|141 |`module-structure.adoc` |`i18n` |Languages and Internationalisation |stays
|142 |`module-structure.adoc` |`dependency-rules` |Dependency Rules |stays
|143 |`module-structure.adoc` |`integration-variants` |Integration Variants |stays
|144 |`module-structure.adoc` |`forgejo` |Codeberg and Forgejo (`pm-provider-forgejo`) |stays
|145 |`module-structure.adoc` |`findings-analysis-contracts` |Findings & Code-Analysis Contracts |stays
|146 |`module-structure.adoc` |`binaries` |Binaries & Build Types |stays
|147 |`module-structure.adoc` |`test-placement` |Test Placement |stays
|148 |`module-structure.adoc` |`module-cuts` |Module Cuts |stays
|149 |`implementation-watch/cli-and-security.adoc` |`_cli_security_implementation_watch` |(title) CLI & Security Implementation Watch |pm-mcp-clients
|150 |`implementation-watch/cli-and-security.adoc` |`_overview` |Overview |pm-mcp-clients
|151 |`implementation-watch/cli-and-security.adoc` |`_command_line_binaries_pm_mcp_pm_operator_installation_layout` |Command-Line Binaries (`pm-mcp`, `pm-operator`) & Installation Layout |pm-mcp-clients
|152 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-10` |PM-WATCH-CLI-10: Siblings are started by path, never through PATH |pm-mcp-clients
|153 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-11` |PM-WATCH-CLI-11: An upgrade never mixes releases |pm-mcp-clients
|154 |`implementation-watch/cli-and-security.adoc` |`_harness_bundle_installer_pm_operator_harness` |Harness Bundle Installer (`pm-operator harness`) |pm-mcp-clients
|155 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-12` |PM-WATCH-CLI-12: The installer touches only the elements it owns |pm-mcp-clients
|156 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-13` |PM-WATCH-CLI-13: Weakening an enforcement rule always needs the terminal |pm-mcp-clients
|157 |`implementation-watch/cli-and-security.adoc` |`_operator_credential_acts_runtime_secret_redaction` |Operator Credential Acts & Runtime Secret Redaction |pm-mcp-clients
|158 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-28` |PM-WATCH-CLI-28: A job token is redacted by value and, after a restart, by its `pmj_` shape |pm-mcp-clients
|159 |`implementation-watch/cli-and-security.adoc` |`_operator_channel_isolation_operator_acts_pm_operator` |Operator Channel Isolation & Operator Acts (`pm-operator`) |pm-mcp-clients
|160 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-3` |PM-WATCH-CLI-3: Every documented operator command form is executed as a test |pm-mcp-clients
|161 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-14` |PM-WATCH-CLI-14: The operator checks refuse before anything is sent |pm-mcp-clients
|162 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-22` |PM-WATCH-CLI-22: `pm-operator waive` accepts only what the waivable gate list declares |pm-mcp-clients
|163 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-24` |PM-WATCH-CLI-24: A model-relayed operator action is refused |pm-mcp-clients
|164 |`implementation-watch/cli-and-security.adoc` |`_web_access_pairing_pm_operator_web` |Web Access & Pairing (`pm-operator web`) |plan-marshall-mcp
|165 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-15` |PM-WATCH-CLI-15: A pairing code is a single, short-lived, rate-limited secret |plan-marshall-mcp
|166 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-16` |PM-WATCH-CLI-16: A device secret exists only as its hash |plan-marshall-mcp
|167 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-17` |PM-WATCH-CLI-17: Revocation and idle expiry take effect at once |plan-marshall-mcp
|168 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-18` |PM-WATCH-CLI-18: Every web response carries every security header |plan-marshall-mcp
|169 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-19` |PM-WATCH-CLI-19: Foreign hosts and origins are refused before routing |plan-marshall-mcp
|170 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-20` |PM-WATCH-CLI-20: The runtime checks the device secret on every request, and never takes the runtime token over TCP |plan-marshall-mcp
|171 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-21` |PM-WATCH-CLI-21: A web listener exists only while enabled and never routes `/mcp` |plan-marshall-mcp
|172 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-26` |PM-WATCH-CLI-26: The listener decides what is web-originated, never a header |plan-marshall-mcp
|173 |`implementation-watch/cli-and-security.adoc` |`PM-WATCH-CLI-27` |PM-WATCH-CLI-27: The route table of the web listener is a closed allowlist |plan-marshall-mcp
|174 |`implementation-watch/runtime-model.adoc` |`_runtime_model_implementation_watch` |(title) Runtime Model Implementation Watch |plan-marshall-mcp
|175 |`implementation-watch/runtime-model.adoc` |`_overview` |Overview |plan-marshall-mcp
|176 |`implementation-watch/runtime-model.adoc` |`_runtime` |Runtime |plan-marshall-mcp
|177 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-1` |PM-WATCH-RUN-1: Started means reachable |plan-marshall-mcp
|178 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-4` |PM-WATCH-RUN-4: The runtime token is fresh, private, and never leaks |plan-marshall-mcp
|179 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-5` |PM-WATCH-RUN-5: Without a runtime, the runtime-free verbs start nothing and write nothing |plan-marshall-mcp
|180 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-7` |PM-WATCH-RUN-7: A second runtime exits at once, and one on-demand start wins |plan-marshall-mcp
|181 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-12` |PM-WATCH-RUN-12: Every `pm-api` refusal carries a code of the closed list |plan-marshall-mcp
|182 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-14` |PM-WATCH-RUN-14: The first start on a fresh machine creates its base before it locks |plan-marshall-mcp
|183 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-17` |PM-WATCH-RUN-17: The socket is private before it is announced, and no native code runs from the temporary directory |plan-marshall-mcp
|184 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-18` |PM-WATCH-RUN-18: Hidden characters are filtered in the encoder, never in the store |plan-marshall-mcp
|185 |`implementation-watch/runtime-model.adoc` |`_relay` |Relay |pm-mcp-clients
|186 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-16` |PM-WATCH-RUN-16: The listen stream stays silent and `initialize` issues no session |pm-mcp-clients
|187 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-10` |PM-WATCH-RUN-10: A harness cancel of an answered request cancels nothing |pm-mcp-clients
|188 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-11` |PM-WATCH-RUN-11: Identity arguments never appear in a tool schema |pm-mcp-clients
|189 |`implementation-watch/runtime-model.adoc` |`_verification` |Verification |plan-marshall-mcp
|190 |`implementation-watch/runtime-model.adoc` |`PM-WATCH-RUN-3` |PM-WATCH-RUN-3: Tests never observe machine-wide state |plan-marshall-mcp
|191 |`implementation-watch/harness-integration.adoc` |`_harness_integration_implementation_watch` |(title) Harness Integration Implementation Watch |pm-mcp-clients
|192 |`implementation-watch/harness-integration.adoc` |`_overview` |Overview |pm-mcp-clients
|193 |`implementation-watch/harness-integration.adoc` |`_normative_harness_profile_json_schema` |Normative Harness Profile JSON Schema |pm-mcp-clients
|194 |`implementation-watch/harness-integration.adoc` |`PM-WATCH-CLIENT-4` |PM-WATCH-CLIENT-4: A harness started inside another harness is attributed to the outer one |pm-mcp-clients
|195 |`implementation-watch/harness-integration.adoc` |`_embedded_harness_bundle_bundle_manifest_json_schema` |Embedded Harness Bundle `bundle-manifest.json` Schema |pm-mcp-clients
|196 |`implementation-watch/harness-integration.adoc` |`PM-WATCH-CLIENT-5` |PM-WATCH-CLIENT-5: Harness-side enforcers are scope-bound; only server gates travel with every session |pm-mcp-clients
|197 |`implementation-watch/harness-integration.adoc` |`PM-WATCH-CLIENT-7` |PM-WATCH-CLIENT-7: A permission wildcard ends the rule |pm-mcp-clients
|198 |`implementation-watch/harness-integration.adoc` |`PM-WATCH-CLIENT-9` |PM-WATCH-CLIENT-9: Bundle artifacts never carry a `plan-marshall` name |pm-mcp-clients
|199 |`implementation-watch/module-structure.adoc` |`_module_structure_implementation_watch` |(title) Module Structure Implementation Watch |stays
|200 |`implementation-watch/module-structure.adoc` |`_overview` |Overview |stays
|201 |`implementation-watch/module-structure.adoc` |`_repositories_artifact_flow` |Repositories & Artifact Flow |stays
|202 |`implementation-watch/module-structure.adoc` |`PM-WATCH-MOD-5` |PM-WATCH-MOD-5: Artifacts leave only through the organisation's registry |stays
|203 |`implementation-watch/module-structure.adoc` |`PM-WATCH-MOD-6` |PM-WATCH-MOD-6: No private model-facing content in a public repository |stays
|204 |`implementation-watch/module-structure.adoc` |`_binaries_build_types` |Binaries & Build Types |stays
|205 |`implementation-watch/module-structure.adoc` |`PM-WATCH-MOD-2` |PM-WATCH-MOD-2: The release is exactly four binaries with a complete SBOM |stays

## Totals

|Document |Headings in the source |Rows |
|---|---|---|
|`cli-and-security.adoc` |6 |6 |
|`cli-and-security/01-cli-and-installer.adoc` |17 |17 |
|`cli-and-security/02-operator-channel.adoc` |3 |3 |
|`cli-and-security/03-web-server.adoc` |14 |14 |
|`runtime-model.adoc` |6 |6 |
|`runtime-model/01-processes-and-transport.adoc` |27 |27 |
|`runtime-model/03-relay.adoc` |11 |11 |
|`runtime-model/05-mock-runtime.adoc` |7 |7 |
|`runtime-model/06-event-catalogue.adoc` |8 |8 |
|`harness-integration.adoc` |6 |6 |
|`harness-integration/01-harness-detection.adoc` |4 |4 |
|`harness-integration/02-bundle-manifest.adoc` |6 |6 |
|`mcp-tools.adoc` |6 |6 |
|`mcp-tools/04-errors-and-testing.adoc` |9 |9 |
|`module-structure.adoc` |18 |18 |
|`implementation-watch/cli-and-security.adoc` |25 |25 |
|`implementation-watch/runtime-model.adoc` |17 |17 |
|`implementation-watch/harness-integration.adoc` |8 |8 |
|`implementation-watch/module-structure.adoc` |7 |7 |
|**Total** |**205** |**205** |

Homes in totals: `pm-mcp-clients` 88 rows, `plan-marshall-mcp` 92 rows, `stays` 25 rows.

## Documents that stay untouched

- `implementation-watch/cross-cutting.adoc`: the operator's decision (O1-A) keeps it in
  `plan-marshall-documentation`; no section of it is moved and no row is needed.
- `runtime-model/04-sessions-and-workers.adoc`: the daemon's (its plain plan moves it to
  `plan-marshall-mcp`); it is not in the `Specified in` column of a module of this repository.
- `evaluation.adoc`: the daemon's, as the standing constraint states.

## The split of a family

- `cli-and-security`: `01` moves whole (the client binaries, the installer, the entry); `02` moves whole (the
  operator's credential acts and the operator channel, whose runtime-side statements are already specified in
  `pm-mcp-core`); `03` splits at `#web-app` (the only section the Modules table gives `pm-web-app` and
  `pm-web-e2e`). The family index, its `Overview`, `Traceability` and `Status` stay with the daemon's index;
  this repository creates `doc/specification/cli-and-security.adoc` listing only the parts it holds. The
  standing constraint's "server side of `02`" resolves to no separate section: every server-side statement of
  `02` is a cross-reference into `pm-mcp-core` (credential stores, redaction, identifier registry), and both
  sections name operator verbs of `pm-operator`.
- `runtime-model`: `03`, `05`, `06` move whole; `01` stays with the daemon, although `pm-api` implements its
  `#local-api` and `#_start_modes` — the client-relevant statements there are the API contract, which the
  daemon's document states and this repository links. The family index stays with the daemon; this
  repository creates `doc/specification/runtime-model.adoc` for the three parts.
- `harness-integration`: moves whole. The spec's HYPOTHESIS is **confirmed**: `02` § what the bundle manifest
  says the installer reads is `pm-operator harness install|update|remove|status` and the runtime act behind the
  web app (`pm-operator` and `pm-api` of this repository), while the bundle *templates* — the files under
  `classpath:/bundles/<harness_id>/` — are the private model-facing content of `plan-marshall-mcp`
  (`module-structure.adoc#model-facing-content`). Split at that line: `#bundle-content` (what a bundle carries)
  goes to `plan-marshall-mcp`; the manifest schema, the example, the ownership and digest rules and the drift
  checks stay here.

## Appendix: block anchors that are not headings

They travel with their section and keep their id: `cli-and-installer` `[[devices]]`-style inline anchors
(`fresh-code`, `web-open`), `02` (`operator-credential-cli`'s items `operator-integrity-hold`, `operator-waive`,
`operator-accept-edit`, `operator-abort`, `operator-grant-access`, `operator-enrol-harness`, `operator-roles`),
`03` (`devices`), `harness-integration/01` (`harness-detection-table`, `nested-harnesses`, `ai-agent-mapping`,
`codex-environment`, `antigravity-environment`, `instruction-files`), `harness-integration/02`
(`bundle-manifest-scopes`, `recommended-permission-rules`, `shim-skill`), `mcp-tools/04`
(`relay-transport-codes`).
