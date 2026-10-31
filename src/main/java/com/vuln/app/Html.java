package com.vuln.app;

/** Design/layout helper for VulnBank — "The Vault" story mode. Markup only; sinks live in the controller. */
public final class Html {
    public static final String[][] CH = {
        //  no, route, title, level, owasp, desc, story
        {"1","login","The Lobby Terminal","easy","A03 · SQL Injection",
         "Meridian Bank's online login trusts its own query. Walk in without a password.",
         "The teller terminal builds its login check by gluing your input into SQL. Glue in something clever."},
        {"2","greet","Personalised Greeting","easy","A03 · Reflected XSS",
         "The dashboard greets you by name — exactly as you typed it, tags and all.",
         "Your name is printed back into the page with no filter. Hand it a script instead of a name."},
        {"3","account","Someone Else's Vault","medium","A01 · IDOR",
         "Account statements load by id. No one checks the id is yours.",
         "Your account is 1001. The id in the URL is the only lock — and it isn't locked."},
        {"4","xml","SWIFT Import","medium","A05 · XXE",
         "The transfer importer parses raw XML with entities fully enabled.",
         "Feed the importer a DOCTYPE with an external entity and the parser will read files for you."},
        {"5","fetch","Link Verifier","medium","A10 · SSRF",
         "A tool that fetches any URL you give it — including ones meant to stay internal.",
         "The server fetches your URL itself. Point it inward, at metadata the bank never meant to expose."},
        {"6","report","Statement Export","hard","A01 · Path Traversal",
         "Exports read a file by name from the reports folder. Names can climb.",
         "The exporter joins your filename onto a base path without checking it stays inside."},
        {"7","eval","The Rules Engine","insane","A03 · SpEL Injection",
         "An 'advanced formula' box evaluates expressions server-side. All of them.",
         "The formula box parses your text as a Spring expression and runs it. Expressions can reach Runtime."}
    };

    public static String badge(String l){
        String t = l.toUpperCase();
        return "<span class='badge b-"+l+"'>"+t+"</span>";
    }

    public static String head(String active){
        StringBuilder nav = new StringBuilder();
        nav.append("<a href='/' class='").append(active.equals("home")?"on":"").append("'>Briefing</a>");
        for(String[] c: CH)
            nav.append("<a href='/").append(c[1]).append("' class='")
               .append(active.equals(c[1])?"on":"").append("'>").append(c[0]).append("</a>");
        return "<!doctype html><html lang=en><head><meta charset=utf-8>"
            + "<meta name=viewport content='width=device-width,initial-scale=1'>"
            + "<title>VulnBank · The Vault</title><link rel=stylesheet href='/style.css'></head><body>"
            + "<header class=topbar><div class=wrap>"
            + "<div class=brand><span class=dot></span> VulnBank <small>· Java target</small></div>"
            + "<nav>"+nav+"</nav></div></header><div class=wrap>";
    }
    public static String foot(){
        return "</div><footer><div class=wrap>VulnBank — deliberately vulnerable training target. "
            + "Authorized use only · fixes in <code>solutions/java-solutions.md</code></div></footer></body></html>";
    }
    /** find chapter row by route */
    public static String[] ch(String route){ for(String[] c: CH) if(c[1].equals(route)) return c; return null; }
    public static String panelOpen(String[] c){
        return "<div class=panel><div style='display:flex;gap:10px;align-items:center;margin-bottom:6px'>"
            + "<span class=tag>Chapter "+c[0]+"</span>"+badge(c[3])
            + "</div>"
            + "<h2>"+c[2]+"</h2><p class=story>"+c[6]+"</p>";
    }
    public static String out(String body){ return "<div class=out>"+body+"</div>"; }
    public static String hint(String body){ return ""; }
    private Html(){}
}
