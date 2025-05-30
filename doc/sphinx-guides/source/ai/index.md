# AI Guide

Artificial Intelligence (AI) is a growing component of the Dataverse ecosystem.

```{contents} Contents:
:local:
:depth: 2
```

## Tools

### Ask the Data

Ask the Data is an {ref}`external tool <inventory-of-external-tools>` that allows you ask natural language questions about the data contained in Dataverse tables (tabular data). See the README.md file at <https://github.com/IQSS/askdataverse/tree/main/askthedata> for the instructions on adding Ask the Data to your Dataverse installation.

### TurboCurator

TurboCurator is an {ref}`external tool <inventory-of-external-tools>` that generates metadata improvements for title, description, and keywords. It relies on OpenAI's ChatGPT & ICPSR best practices. See the [TurboCurator Dataverse Administrator](https://turbocurator.icpsr.umich.edu/tc/adminabout/) page for more details on how it works and adding TurboCurator to your Dataverse installation.

### Ask Dataverse

Ask Dataverse ([ask.dataverse.org](https://ask.dataverse.org)) is a place to ask questions about the Dataverse Project and the Dataverse software. It was created by Slava Tykhonov who [announced](https://groups.google.com/g/dataverse-community/c/tqwCoygO4oE/m/MNSfrw_QAwAJ) it in December 2024 and presented it February 2025 ([video](https://harvard.zoom.us/rec/share/bOizatNdMdxINRCnqpt87fPITPvsDWTv3ysvA8kIaEE4wnmZPSeSUkdmpKYP1ooA.rKoNMqED_L8KtHOi), [slides](https://docs.google.com/presentation/d/1HFN-wAe4eUGwJAhYCLbNcNHAsi-Hy8jQ/edit?usp=sharing&ouid=117275479921759507378&rtpof=true&sd=true), [notes](https://docs.google.com/document/d/1Dz07WKceGrBGdq5wWf0NJS08CO0FEmi4TgQBcsDcpRE/edit?usp=sharing)).

### DataChat

DataChat is a multilingual open source natural language interface for Dataverse and other data platforms with an experimental Graph AI implementation for Croissant support. DataChat can literally talk back to you and explain what is inside of every single dataset, you can ask any question and it responds on the level of metadata described by Croissant standard. Learn more at <https://github.com/gdcc/datachat>.

## Protocols

(mcp)=
### Model Context Protocol (MCP)

[Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction) is a standard for AI Agents to communicate with tools and services, [announced](https://www.anthropic.com/news/model-context-protocol) in November 2024.

An MCP server for Dataverse has been deployed to [mcp.dataverse.org][], powered by the code at <https://github.com/gdcc/mcp-dataverse>. See the code's README for information on configuring MCP clients (e.g. Cursor, Visual Studio Code, Windsurf, Zed, etc.) to use [mcp.dataverse.org][] or your own local installation (setup instructions are also provided).

[mcp.dataverse.org]: https://mcp.dataverse.org
