#!/usr/bin/env bash
#
# Endless Domains Claude Code Stack Installer
# Installs: Superpowers, n8n-MCP + n8n-skills, Obsidian Skills
# Author: prepared for Vicky / Endless Domains
#
# Run this once on the machine where you use Claude Code.
# Requires: Claude Code installed, Node.js 18+, git, npm.

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${BLUE}==>${NC} $1"; }
ok()  { echo -e "${GREEN}✓${NC} $1"; }
warn(){ echo -e "${YELLOW}!${NC} $1"; }
err() { echo -e "${RED}✗${NC} $1"; }

log "Endless Domains Claude Code stack installer starting"
echo ""

# Preflight checks
log "Checking prerequisites"

if ! command -v claude &>/dev/null; then
    err "Claude Code CLI not found. Install from https://docs.claude.com/en/docs/claude-code first."
    exit 1
fi
ok "Claude Code found: $(claude --version 2>/dev/null || echo 'detected')"

if ! command -v node &>/dev/null; then
    err "Node.js not found. Install Node 18+ first."
    exit 1
fi

NODE_MAJOR=$(node -v | sed 's/v//' | cut -d. -f1)
if [ "$NODE_MAJOR" -lt 18 ]; then
    err "Node.js 18+ required. Current: $(node -v)"
    exit 1
fi
ok "Node.js $(node -v)"

if ! command -v git &>/dev/null; then
    err "git not found. Install git first."
    exit 1
fi
ok "git found"

echo ""

# 1. Superpowers - via Claude Code plugin marketplace (cleanest install)
log "Installing Superpowers (TDD, brainstorm, plan, execute workflow)"
echo ""
echo "Run these two commands inside Claude Code after this script finishes:"
echo ""
echo -e "  ${YELLOW}/plugin marketplace add obra/superpowers-marketplace${NC}"
echo -e "  ${YELLOW}/plugin install superpowers@superpowers-marketplace${NC}"
echo ""
ok "Superpowers install instructions noted"
echo ""

# 2. Obsidian Skills - via Claude Code plugin marketplace
log "Installing Obsidian Skills (kepano - second brain layer)"
echo ""
echo "If you use Obsidian, run inside Claude Code:"
echo ""
echo -e "  ${YELLOW}/plugin marketplace add kepano/obsidian-skills${NC}"
echo -e "  ${YELLOW}/plugin install obsidian@obsidian-skills${NC}"
echo ""
echo "Then point Claude Code at your vault directory when working with notes."
ok "Obsidian Skills install instructions noted"
echo ""

# 3. n8n-MCP - the operationally important one for your analytics stack
log "Installing n8n-MCP server (workflow automation knowledge)"

CLAUDE_CONFIG_DIR=""
case "$(uname -s)" in
    Darwin*) CLAUDE_CONFIG_DIR="$HOME/Library/Application Support/Claude" ;;
    Linux*)  CLAUDE_CONFIG_DIR="$HOME/.config/Claude" ;;
    MINGW*|CYGWIN*|MSYS*) CLAUDE_CONFIG_DIR="$APPDATA/Claude" ;;
esac

mkdir -p "$CLAUDE_CONFIG_DIR"

INSTALL_DIR="$HOME/.endless-stack/n8n-mcp"
mkdir -p "$(dirname "$INSTALL_DIR")"

if [ -d "$INSTALL_DIR" ]; then
    log "n8n-mcp already cloned, pulling latest"
    cd "$INSTALL_DIR" && git pull --quiet
else
    log "Cloning n8n-mcp"
    git clone --quiet https://github.com/czlonkowski/n8n-mcp.git "$INSTALL_DIR"
    cd "$INSTALL_DIR"
fi

log "Building n8n-mcp (this takes a minute)"
npm install
npm run build
npm run rebuild 2>/dev/null || true
ok "n8n-mcp built at $INSTALL_DIR"
echo ""

# Generate the Claude Desktop config snippet
CONFIG_SNIPPET="$HOME/.endless-stack/claude_desktop_config_snippet.json"
cat > "$CONFIG_SNIPPET" <<EOF
{
  "mcpServers": {
    "n8n-mcp": {
      "command": "node",
      "args": ["$INSTALL_DIR/dist/mcp/index.js"],
      "env": {
        "NODE_ENV": "production",
        "LOG_LEVEL": "error",
        "MCP_MODE": "stdio",
        "DISABLE_CONSOLE_OUTPUT": "true"
      }
    }
  }
}
EOF

ok "Config snippet written to $CONFIG_SNIPPET"
echo ""
log "MERGE this snippet into your Claude Desktop config at:"
echo "    $CLAUDE_CONFIG_DIR/claude_desktop_config.json"
echo ""
echo "If the file doesn't exist yet, just copy the snippet there directly."
echo ""

# n8n-skills companion (optional but recommended)
log "Installing n8n-skills companion (workflow patterns + validation)"
echo ""
echo "Inside Claude Code, run:"
echo ""
echo -e "  ${YELLOW}/plugin marketplace add czlonkowski/n8n-skills${NC}"
echo -e "  ${YELLOW}/plugin install n8n-mcp-skills${NC}"
echo ""
ok "n8n-skills install instructions noted"
echo ""

echo "════════════════════════════════════════════════════════════════"
echo -e "${GREEN}Install prep complete.${NC}"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "NEXT STEPS:"
echo ""
echo "1. Restart Claude Desktop completely (quit, not just close window)"
echo ""
echo "2. Open Claude Code and run the four /plugin commands shown above"
echo "   (Superpowers, Obsidian Skills, n8n-skills)"
echo ""
echo "3. Test n8n-MCP is live: ask Claude 'list available n8n nodes'"
echo "   — if it can answer with real node names, the MCP is working."
echo ""
echo "4. Test Superpowers is live: start a new Claude Code session and"
echo "   ask it to build something. It should auto-trigger brainstorming."
echo ""
echo "If anything breaks, the n8n-mcp install lives at:"
echo "    $INSTALL_DIR"
echo "and the config snippet at:"
echo "    $CONFIG_SNIPPET"
echo ""
