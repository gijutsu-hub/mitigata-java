package com.vuln.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

// ============================================================
//  VulnBank (Java/Spring Boot) — "The Vault" story-mode lab.
//  DELIBERATELY VULNERABLE. Layout in Html.java; sinks are here.
// ============================================================
@RestController
public class VulnController {

    @Autowired JdbcTemplate jdbc;

    private static String esc(String s){ return s==null? "" :
        s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;"); }

    // ---------------- Story landing ----------------
    @GetMapping(value="/", produces="text/html")
    public String home() {
        StringBuilder g = new StringBuilder();
        for (String[] c : Html.CH) {
            g.append("<div class=chapter><div class=no>Chapter ").append(c[0]).append("</div>")
             .append("<div class=meta>").append(Html.badge(c[3]))
             .append("</div>")
             .append("<h3>").append(c[2]).append("</h3><div class=desc>").append(c[5]).append("</div>")
             .append("<a class=btn href='/").append(c[1]).append("'>Enter chapter →</a></div>");
        }
        StringBuilder rail = new StringBuilder();
        for (int i=0;i<Html.CH.length;i++) rail.append("<div class='step").append(i==0?" done":"").append("'></div>");
        return Html.head("home")
            + "<section class=hero><div class=kicker>Mission Briefing</div>"
            + "<h1>The&nbsp;Vault</h1><p>Meridian Bank believes its online platform is airtight. "
            + "Your engagement: breach it one chapter at a time — from the lobby terminal to the rules "
            + "engine in the back office. Seven chapters, <b>Easy</b> climbing to <b>Insane</b>.</p></section>"
            + "<div class=rail>"+rail+"</div>"
            + "<div class=grid>"+g+"</div>"
            + "<div class=warn>⚠ Intentionally insecure. Run only on an isolated host.</div>"
            + Html.foot();
    }

    // ---------------- Ch1 · SQL Injection (EASY) ----------------
    @GetMapping(value="/login", produces="text/html")
    public String login(@RequestParam(required=false) String username,
                        @RequestParam(required=false) String password) {
        String[] c = Html.ch("login");
        StringBuilder r = new StringBuilder(Html.head("login")).append(Html.panelOpen(c));
        r.append("<form class=vf method=get>")
         .append("<div><label>Username</label><input name=username value='").append(esc(username)).append("' placeholder='alice'></div>")
         .append("<div><label>Password</label><input name=password type=password placeholder='••••••••'></div>")
         .append("<div class=row><button class=btn>Sign in</button><a class='btn ghost' href='/'>Abort</a></div></form>");
        if (username != null) {
            String sql = "SELECT username, role FROM users WHERE username = '"
                       + username + "' AND password = '" + password + "'";
            try {
                List<Map<String,Object>> rows = jdbc.queryForList(sql);
                if (rows.isEmpty()) r.append(Html.out("✘ Invalid credentials.\n<!-- "+esc(sql)+" -->"));
                else r.append("<div class='out ok'>✔ Authenticated as <b>"+esc(""+rows.get(0).get("username"))
                        +"</b> (role="+esc(""+rows.get(0).get("role"))+")</div>");
            } catch (Exception e) {
                r.append(Html.out("DB error: "+esc(e.getMessage())+"\nSQL: "+esc(sql))); // A05 verbose error
            }
        }
        return r.append(Html.foot()).toString();
    }

    // ---------------- Ch2 · Reflected XSS (EASY) ----------------
    @GetMapping(value="/greet", produces="text/html")
    public String greet(@RequestParam(required=false) String name) {
        String[] c = Html.ch("greet");
        StringBuilder r = new StringBuilder(Html.head("greet")).append(Html.panelOpen(c));
        r.append("<form class=vf method=get><div><label>Your name</label>")
         .append("<input name=name value='").append(name==null?"":name).append("' placeholder='type your name'></div>")
         .append("<button class=btn>Greet me</button></form>");
        if (name != null) r.append("<div class=out>Hello, " + name + "!</div>");
        return r.append(Html.foot()).toString();
    }

    // ---------------- Ch3 · IDOR (MEDIUM) ----------------
    @GetMapping(value="/account", produces="text/html")
    public String account(@RequestParam(required=false, defaultValue="1001") int id) {
        String[] c = Html.ch("account");
        StringBuilder r = new StringBuilder(Html.head("account")).append(Html.panelOpen(c));
        r.append("<form class=vf method=get><div><label>Account id</label>")
         .append("<input name=id value='").append(id).append("'></div><button class=btn>Open statement</button></form>");
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM accounts WHERE id = ?", id);
        r.append(Html.out(rows.isEmpty() ? "No such account." : esc(rows.get(0).toString())));
        return r.append(Html.foot()).toString();
    }

    // ---------------- Ch4 · XXE (MEDIUM) ----------------
    @RequestMapping(value="/xml", produces="text/html", method={RequestMethod.GET, RequestMethod.POST})
    public String xml(@RequestParam(required=false) String xml) throws Exception {
        String[] c = Html.ch("xml");
        StringBuilder r = new StringBuilder(Html.head("xml")).append(Html.panelOpen(c));
        String sample = "<?xml version=\"1.0\"?>\n<transfer><memo>hello</memo></transfer>";
        r.append("<form class=vf method=post style='max-width:620px'><div><label>SWIFT XML payload</label>")
         .append("<textarea name=xml rows=7 placeholder='paste XML'>").append(xml==null?esc(sample):esc(xml))
         .append("</textarea></div><button class=btn>Import</button></form>");
        if (xml != null && !xml.isEmpty()) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(new ByteArrayInputStream(xml.getBytes()));
                r.append(Html.out("Parsed content: " + esc(doc.getDocumentElement().getTextContent())));
            } catch (Exception e) { r.append(Html.out("Parse error: " + esc(e.getMessage()))); }
        }
        return r.append(Html.foot()).toString();
    }

    // ---------------- Ch5 · SSRF (MEDIUM) ----------------
    @GetMapping(value="/fetch", produces="text/html")
    public String fetch(@RequestParam(required=false) String url) {
        String[] c = Html.ch("fetch");
        StringBuilder r = new StringBuilder(Html.head("fetch")).append(Html.panelOpen(c));
        r.append("<form class=vf method=get style='max-width:620px'><div><label>URL to verify</label>")
         .append("<input name=url value='").append(esc(url)).append("' placeholder='https://example.com'></div>")
         .append("<button class=btn>Verify link</button></form>");
        if (url != null && !url.isEmpty()) {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
                con.setConnectTimeout(3000); con.setReadTimeout(3000);
                try (BufferedReader b = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    r.append(Html.out(esc(b.lines().limit(40).collect(Collectors.joining("\n")))));
                }
            } catch (Exception e) { r.append(Html.out("fetch error: " + esc(e.getMessage()))); }
        }
        return r.append(Html.foot()).toString();
    }

    // ---------------- Ch6 · Path Traversal (HARD) ----------------
    @GetMapping(value="/report", produces="text/html")
    public String report(@RequestParam(required=false, defaultValue="summary.txt") String file) {
        String[] c = Html.ch("report");
        StringBuilder r = new StringBuilder(Html.head("report")).append(Html.panelOpen(c));
        r.append("<form class=vf method=get><div><label>Report file</label>")
         .append("<input name=file value='").append(esc(file)).append("'></div><button class=btn>Export</button></form>");
        try {
            Path base = Paths.get("/app/reports");
            Path target = base.resolve(file);
            r.append(Html.out(esc(new String(Files.readAllBytes(target)))));
        } catch (Exception e) { r.append(Html.out("read error: " + esc(e.getMessage()))); }
        return r.append(Html.foot()).toString();
    }

    // ---------------- Ch7 · SpEL Injection → RCE (INSANE) ----------------
    @GetMapping(value="/eval", produces="text/html")
    public String eval(@RequestParam(required=false) String expr) {
        String[] c = Html.ch("eval");
        StringBuilder r = new StringBuilder(Html.head("eval")).append(Html.panelOpen(c));
        r.append("<form class=vf method=get style='max-width:620px'><div><label>Formula</label>")
         .append("<input name=expr value='").append(esc(expr)).append("' placeholder='1 + 2 * 3'></div>")
         .append("<button class=btn>Evaluate</button></form>");
        if (expr != null && !expr.isEmpty()) {
            try {
                ExpressionParser p = new SpelExpressionParser();
                Expression e = p.parseExpression(expr);
                r.append(Html.out("Result: " + esc(String.valueOf(e.getValue()))));
            } catch (Exception e) { r.append(Html.out("eval error: " + esc(e.getMessage()))); }
        }
        return r.append(Html.foot()).toString();
    }
}
