package com.microsoftTeams.bot;

import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.integration.BotFrameworkHttpAdapter;
import com.microsoft.bot.integration.Configuration;
import com.microsoft.bot.schema.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller will receive GET requests at /api/notify and send a message
 * to all ConversationReferences.
 *
 * @see ConversationReferences
 * @see JenkinsBot
 * @see Application
 */
@RestController
public class NotifyController {
    /**
     * The BotFrameworkHttpAdapter to use. Note it is provided by dependency
     * injection via the constructor.
     *
     * @see com.microsoft.bot.integration.spring.BotDependencyConfiguration
     */
    private final BotFrameworkHttpAdapter adapter;
    private final ConversationReferences conversationReferences;
    private final String appId;

    @Autowired
    public NotifyController(
        BotFrameworkHttpAdapter withAdapter,
        Configuration withConfiguration,
        ConversationReferences withReferences
    ) {
        adapter = withAdapter;
        conversationReferences = withReferences;
        appId = withConfiguration.getProperty("MicrosoftAppId");
    }

    /**
     * Fetching build related information from the Jenkins the api/notify endpoint
     * @param buildInfo
     */
    @PostMapping("/api/notify")
    public void proactiveMessage(@RequestBody BuildInfo buildInfo) {
        HeroCard heroCard = new HeroCard();
        heroCard.setTitle("Build Notification from Jenkins");
        heroCard.setSubtitle("Hii, " + buildInfo.getBuildUser() + " ( " +  buildInfo.getBuildUserEmail() + " )");
        heroCard.setText("Your Jenkins build, labeled as number " + buildInfo.getBuildNumber() + ", has achieved " + buildInfo.getBuildResult());
        heroCard.setButtons(new CardAction(ActionTypes.OPEN_URL, "View Build", buildInfo.getBuildUrl()));

        // send notification only to the first user for testing purpose
        if(!conversationReferences.isEmpty()) {
            ConversationReference reference = conversationReferences.entrySet().iterator().next().getValue();
            adapter.continueConversation(
                    appId, reference, turnContext -> turnContext.sendActivity(MessageFactory.attachment(heroCard.toAttachment())).thenApply(resourceResponse -> null)
            );
        }
        else{
            System.out.println("Empty");
        }
    }

    /**
     * fetching all the users whose build is overridden in the given time frame
     * @param users
     */
    @PostMapping("/api/check")
    public void allUsersOverridden(@RequestBody  Object users){
        System.out.println(users);
    }
}
