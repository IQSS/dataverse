# IntelliJ Auto-Copy of Webapp Files

When deploying the webapp via Payara Tools, you can use this tool to immediately copy changes to non-code files into the running deployment, instantly seeing changes in your browser.

Note: as this relies on using a Bash shell script, it is pretty much limited to Mac and Linux.
Feel free to extend and provide a PowerShell equivalent!

1. Install the [File Watcher plugin](https://plugins.jetbrains.com/plugin/7177-file-watchers)
2. Import the [watchers.xml](./watchers.xml) file at *File > Settings > Tools > File Watchers*
3. Once you have the deployment running (see Container Guides), editing files at `src/main/webapp` will be copied into the deployment after saving the edited file.

Alternatively, you can add an External tool and trigger via menu or shortcut to do the copying manually:
https://www.jetbrains.com/help/idea/configuring-third-party-tools.html
