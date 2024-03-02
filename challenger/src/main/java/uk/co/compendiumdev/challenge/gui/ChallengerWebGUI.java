package uk.co.compendiumdev.challenge.gui;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.compendiumdev.challenge.CHALLENGE;
import uk.co.compendiumdev.challenge.ChallengerAuthData;
import uk.co.compendiumdev.challenge.challengers.Challengers;
import uk.co.compendiumdev.challenge.challenges.*;
import uk.co.compendiumdev.challenge.persistence.PersistenceLayer;
import uk.co.compendiumdev.challenge.persistence.PersistenceResponse;
import uk.co.compendiumdev.thingifier.core.EntityRelModel;
import uk.co.compendiumdev.thingifier.htmlgui.DefaultGUIHTML;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static spark.Spark.get;
import static spark.Spark.notFound;

public class ChallengerWebGUI {

    Logger logger = LoggerFactory.getLogger(ChallengerWebGUI.class);
    private final DefaultGUIHTML guiManagement;
    private final boolean guiStayAlive;

    public ChallengerWebGUI(final DefaultGUIHTML defaultGui, final boolean guiStayAlive) {
        this.guiManagement = defaultGui;
        this.guiStayAlive = guiStayAlive;
    }

    public void setup(final Challengers challengers, 
                      final ChallengeDefinitions challengeDefinitions, 
                      final PersistenceLayer persistenceLayer, 
                      final boolean single_player_mode) {

        guiManagement.appendMenuItem("Challenges", "/gui/challenges");
        guiManagement.removeMenuItem("Home");
        guiManagement.prefixMenuItem("Home", "/");
        guiManagement.appendMenuItem("Learning", "/learning.html");

        guiManagement.setHomePageContent("    <h2 id=\"challenges\">Challenges</h2>\n" +
                "    <p>The challenges can be completed by issuing API requests to the API.</p>\n" +
                "    <p>e.g. <code>GET http://localhost:4567/todos</code> would complete the challenge to &quot;GET the list of todos&quot;</p>\n" +
                "    <p>You can also <code>GET http://localhost:4567/challenges</code> to get the list of challenges and their status as an API call. </p>\n"
        );

        guiManagement.setFooter(getChallengesFooter());

        // use the index.html to allow easier creation of docs and landing page
//        get("/", (request, result) -> {
//            result.redirect("/gui");
//            return "";
//        });

        // single user / default session
        get("/gui/challenges", (request, result) -> {

            if (request.cookie("X-THINGIFIER-DATABASE-NAME") != null) {
                // we didn't add a challenger in the URL but we do have one in the cookie
                result.header("location", "/gui/challenges/" + request.cookie("X-THINGIFIER-DATABASE-NAME"));
                result.status(302);
                return "";
            }

            result.type("text/html");
            result.status(200);

            StringBuilder html = new StringBuilder();
            html.append(guiManagement.getPageStart("Challenges", "<script src='/js/challengerui.js'></script>"));
            html.append(guiManagement.getMenuAsHTML());

            // todo explain challenges - single user mode


            //List<ChallengeData> reportOn = new ArrayList<>();

            if (single_player_mode) {
                html.append(playerChallengesIntro());
                //reportOn = new ChallengesPayload(challengeDefinitions, challengers.SINGLE_PLAYER).getAsChallenges();
                String json = "{}";
                if(challengers.getErModel().getDatabaseNames().contains(EntityRelModel.DEFAULT_DATABASE_NAME)){
                    json = challengers.getErModel().getInstanceData(EntityRelModel.DEFAULT_DATABASE_NAME).asJson();
                }
                html.append(outputChallengeDataAsJS(challengers.SINGLE_PLAYER, json));
                html.append(renderChallengeData(challengeDefinitions, challengers.SINGLE_PLAYER));
                html.append(injectCookieFunctions());
                html.append(storeThingifierDatabaseNameCookie(challengers.SINGLE_PLAYER.getXChallenger()));
            } else {
                html.append("<div style='clear:both'><p><strong>Unknown Challenger ID</strong></p></div>");
                html.append(outputChallengeDataAsJS(challengers.SINGLE_PLAYER, "{}"));
                html.append(multiUserShortHelp());
                html.append(injectCookieFunctions());
                html.append(showPreviousGuids());
                html.append(inputAChallengeGuidScript());

                //reportOn = new ChallengesPayload(challengeDefinitions, challengers.DEFAULT_PLAYER_DATA).getAsChallenges();
                html.append(renderChallengeData(challengeDefinitions, challengers.DEFAULT_PLAYER_DATA));
            }

            //html.append(renderChallengeData(reportOn));

            html.append(guiManagement.getPageFooter());
            html.append(guiManagement.getPageEnd());
            return html.toString();
        });

        // multi user
        get("/gui/challenges/*", (request, result) -> {

            result.type("text/html");
            result.status(200);

            StringBuilder html = new StringBuilder();
            html.append(guiManagement.getPageStart("Challenges", "<script src='/js/challengerui.js'></script>"));
            html.append(guiManagement.getMenuAsHTML());

            html.append(playerChallengesIntro());


            //List<ChallengeData> reportOn = null;

            String xChallenger = null;

            try {
                xChallenger = request.splat()[0];
            } catch (Exception e) {
                logger.warn("No challenger id to render");
            }


            // is there an in memory challenger with this id?
            ChallengerAuthData challenger = null;
            PersistenceResponse persistence = null;

            // only check if an xchallenger was passed in
            if (xChallenger != null && !xChallenger.trim().isEmpty()) {
                xChallenger = santitizeChallengerGuid(xChallenger);
                challenger = challengers.getChallenger(xChallenger);

                persistence = new PersistenceResponse();

                // if no inmemory challenger then ask the persistence layer
                if (challenger == null) {
                    persistence = persistenceLayer.tryToLoadChallenger(challengers, xChallenger);
                }
            }

            if (challenger == null) {
                String persistenceReason = "";
                if (persistence != null) {
                    persistenceReason = persistence.getErrorMessage();
                }
                html.append(String.format("<p><strong>Unknown Challenger ID %s</strong></p>",
                        persistenceReason));
                html.append(multiUserShortHelp());
                html.append(injectCookieFunctions());
                html.append(showPreviousGuids());
                html.append(inputAChallengeGuidScript());
                html.append(outputChallengeDataAsJS(challengers.DEFAULT_PLAYER_DATA, "{}"));
                html.append(renderChallengeData(challengeDefinitions, challengers.DEFAULT_PLAYER_DATA));
            } else {
                html.append(injectCookieFunctions());

                String json = "{}";
                if(challengers.getErModel().getDatabaseNames().contains(xChallenger)){
                    json = challengers.getErModel().getInstanceData(xChallenger).asJson();
                }
                html.append(outputChallengeDataAsJS(challenger, json));

                if (!single_player_mode) {
                    html.append(storeThingifierDatabaseNameCookie(xChallenger));
                    html.append(storeCurrentGuidInLocalStorage(xChallenger));

                    html.append(String.format("<p><strong>Progress For Challenger ID %s</strong></p>", xChallenger));
                    html.append(showCurrentStatus());

                    html.append(showPreviousGuids());
                    html.append(inputAChallengeGuidScript());
                }else {
                    html.append(storeThingifierDatabaseNameCookie(xChallenger));
                    html.append(storeCurrentGuidInLocalStorage(xChallenger));
                    html.append(showCurrentStatus());
                }

                html.append(renderChallengeData(challengeDefinitions, challenger));

                html.append(refreshScriptFor(challenger.getXChallenger()));
            }

            //html.append(renderChallengeData(reportOn));

            html.append(guiManagement.getPageFooter());
            html.append(guiManagement.getPageEnd());
            return html.toString();
        });

        get("/gui/404", (request, result) -> {
            result.status(404);
            result.type("text/html");

            StringBuilder html = new StringBuilder();
            html.append(guiManagement.getPageStart("404 Not Found",""));
            html.append(guiManagement.getMenuAsHTML());
            html.append("<h1>Page Not Found</h1>");
            html.append(guiManagement.getPageFooter());
            html.append(guiManagement.getPageEnd());
            return html.toString();
        });

        get("/gui/404/*", (request, result) -> {
            result.status(404);
            result.type("text/html");

            String urltoshow = "";

            try {
                urltoshow = request.splat()[0];
            } catch (Exception e) {
               logger.error("No url to pretend to be on 404", e);
            }

            StringBuilder html = new StringBuilder();
            html.append(guiManagement.getPageStart("404 Not Found", ""));
            html.append(guiManagement.getMenuAsHTML());
            html.append("<h1>Page Not Found</h1>");
            html.append("<script>window.history.pushState({id:\"404sim\"},\"\",\"/" + urltoshow + "\");</script>");
            html.append(guiManagement.getPageFooter());
            html.append(guiManagement.getPageEnd());
            return html.toString();
        });

    }

    private String showCurrentStatus() {
        return "<script>showCurrentStatus()</script>";
    }

    private String santitizeChallengerGuid(String xChallenger) {
        return xChallenger.replaceAll("[^\\-a-zA-Z0-9]","");
    }

    private String injectCookieFunctions(){
        return "";
    }
    private String storeThingifierDatabaseNameCookie(String xChallenger) {
        return "<script>" +
                "setCookie('X-THINGIFIER-DATABASE-NAME','" + xChallenger +"',365);"+
                "</script>";
    }


    private String storeCurrentGuidInLocalStorage(final String xChallenger) {
        return "<script>" +
                "var guids = localStorage.getItem('challenges-guids') || '';" +
                String.format("if(guids==null || !guids.includes('|%s|')){", xChallenger) +
                String.format("localStorage.setItem('challenges-guids',guids + '|%s|');",xChallenger) +
                "}" +
                "</script>";
    }

    private String inputAChallengeGuidScript() {
        return "<p><button onclick=inputChallengeGuid()>Input a Challenger GUID to use</button></p>";
    }

    private String showPreviousGuids() {
        return "<script>displayLocalGuids()</script>";// +
    }

    private String getChallengesFooter() {
        return "<p>&nbsp;</p><hr/><div class='footer'><p>Copyright Compendium Developments Ltd 2020 </p>\n" +
                "<ul class='footerlinks'><li><a href='https://eviltester.com/apichallenges'>API Challenges Info</a></li>\n" +
                "<li><a href='https://eviltester.com'>EvilTester.com</a></li>\n" +
                "</ul></div>";
    }

    private String playerChallengesIntro() {
        final StringBuilder html = new StringBuilder();
        html.append("<div style='clear:both'>");
        html.append("<p>Use the Descriptions of the challenges below to explore the API and solve the challenges." +
                    " Remember to use the API documentation to see the format of POST requests.</p>");
        html.append("</div>");
        return html.toString();
    }

    private String multiUserShortHelp() {
        final StringBuilder html = new StringBuilder();
        html.append("<div style='clear:both' class='headertextblock'>");
        html.append("<p>To view your challenges status in multi-user mode, make sure you have registered as a challenger using a `POST` request to `/challenger` and are including an `X-CHALLENGER` header in all your requests.</p>");
        html.append("<p>Then view the challenges in the GUI by visiting `/gui/challenges/{GUID}`, where `{GUID}` is the value in the `X-CHALLENGER` header.<p>");
        html.append("<p>Challenger sessions are purged from the server memory after 10 minutes of inactivity. To restore your session progress issue an API request with the X-CHALLENGER header (note this will restore the completion state of challenges, but not the data you were using).<p>");
        html.append("<p>You can find more information about this on the <a href='multiuser.html'>Multi User Help Page</a><p>");
        html.append("</div>");
        return html.toString();
    }

    private String refreshScriptFor(final String xChallenger) {

        if(!guiStayAlive)
            return "";

        StringBuilder html = new StringBuilder();

        html.append("<script>");
        html.append("/* keep session alive */");
        html.append("setInterval(function(){");
        html.append("var oReq = new XMLHttpRequest();\n" +
                "oReq.open('GET', '/challenger/" + xChallenger +"');\n" +
                "oReq.send();");
        html.append("},300000);");
        html.append("</script>");
        return html.toString();
    }


    private String renderChallengeData(final List<ChallengeDefinitionData> reportOn) {
        StringBuilder html = new StringBuilder();

        html.append("<table>");
        html.append("<thead>");
        html.append("<tr>");

        html.append("<style>.statustrue{background:palegreen}</style>");
        html.append("<th>ID</th>");
        html.append("<th>Challenge</th>");
        html.append("<th>Done</th>");
        html.append("<th>Description</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        for(ChallengeDefinitionData challenge : reportOn){
            html.append(String.format("<tr class='status%b'>", challenge.status));
            html.append(String.format("<td>%s</td>", challenge.id));
            html.append(String.format("<td>%s</td>", challenge.name));
            html.append(String.format("<td>%b</td>", challenge.status));

            String descriptionHTML = String.format("<p>%s</p>",challenge.description);
            if(challenge.hasHints() || challenge.hasSolutionLinks()){
                descriptionHTML = descriptionHTML + "<br/>";
            }
            if(challenge.hasHints()){
                descriptionHTML = descriptionHTML + "<details><summary>Hints</summary>";
                descriptionHTML = descriptionHTML + "<ul>";
                String hintHtml = "";
                for(ChallengeHint hint : challenge.hints){
                    hintHtml = hintHtml + "<li>" + hint.hintText;
                    if(hint.hintLink!=null && hint.hintLink.length()>0){
                        hintHtml = hintHtml +
                                String.format(" <a href='%s' target='_blank'>Learn More</a>",
                                        hint.hintLink);
                    }
                    hintHtml = hintHtml + "</li>";
                }
                descriptionHTML = descriptionHTML + hintHtml + "</ul>";
                descriptionHTML = descriptionHTML + "</details>";
            }
            if(challenge.hasSolutionLinks()){

                descriptionHTML = descriptionHTML + "<details><summary>Solution</summary>";
                descriptionHTML = descriptionHTML + "<ul>";
                String solutionsHtml = "";
                for(ChallengeSolutionLink solution : challenge.solutions){
                    solutionsHtml = solutionsHtml + "<li>" + solution.asHtmlAHref() + "</li>";
                }
                descriptionHTML = descriptionHTML + solutionsHtml + "</ul>";
                descriptionHTML = descriptionHTML + "</details>";
            }

            html.append(String.format("<td>%s</td>", descriptionHTML));
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        return html.toString();
    }

    private String outputChallengeDataAsJS(final ChallengerAuthData challenger, String json){

        StringBuilder html = new StringBuilder();

        // add the challenge data as JSON
        final String dataString = new Gson().toJson(challenger);
        html.append("<script>document.challengerData=" + dataString + ";</script>");
        // add the current todos as JSON
        html.append("<script>document.databaseData=" + json + ";</script>");

        return html.toString();

    }

    private String renderChallengeData(final ChallengeDefinitions challengeDefinitions, final ChallengerAuthData challenger) {
        StringBuilder html = new StringBuilder();



        final Collection<ChallengeSection> sections = challengeDefinitions.getChallengeSections();

        // add a toc
        html.append("<p id='toc'><strong>Sections</strong></p>");
        html.append("<ul>");
        for(ChallengeSection section : sections){
            html.append(String.format("<li><a href='#%s'>%s</a></li>", section.getTitle().replaceAll(" ", "").toLowerCase(), section.getTitle()));
        }
        html.append("</ul>");

        for(ChallengeSection section : sections){

            html.append(String.format("<h2 id='%s'>", section.getTitle().replaceAll(" ", "").toLowerCase()) + section.getTitle() + "</h2>");
            html.append("<p class='challengesectiondescription'>" + section.getDescription() + "</p>");

            List<ChallengeDefinitionData> sectionData = new ArrayList<>();
            for(ChallengeDefinitionData challenge : section.getChallenges()){
                final ChallengeDefinitionData data = new ChallengeDefinitionData(challenge.id, challenge.name, challenge.description);
                CHALLENGE challengeKey = challengeDefinitions.getChallenge(challenge.name);
                if(challenge!=null) {
                    data.status = challenger.statusOfChallenge(challengeKey);
                    data.addHints(challenge.hints);
                    data.addSolutions(challenge.solutions);
                }
                sectionData.add(data);
            }

            html.append(renderChallengeData(sectionData));
            html.append("<p><a href='#toc'>Back to Section List</a></p>");
        }

        return html.toString();
    }

}
