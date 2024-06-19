package com.microsoftTeams.bot;

import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.microsoft.bot.builder.ActivityHandler;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.schema.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.time.*;

/**
 * This class implements the functionality of the Bot.
 *
 * <p>
 * This is where application specific logic for interacting with the users would
 * be added. In this, the {@link #onMessageActivity(TurnContext)} takes argument for locking components
 * and share locking information to the user and updates the shared
 * {@link ConversationReferences}. The
 * {@link #onMembersAdded(List, TurnContext)} will send a greeting to new
 * conversation participants with instructions for sending a proactive message.
 * </p>
 */
public class JenkinsBot extends ActivityHandler {
    @Value("${server.port:3978}")
    private int port;

    // Message to send to users when the bot receives a Conversation Update event
    private final String welcomeMessage =
        "Successfully added, we will notify you for your build related information of Jenkins.\n" + "\nThanks!!";

    private final String notify = "Please use the available commands: \n" +
            "\n1.add <componentName> (to add some component in the library) \n" +
            "\n2.lock <componentName> (to lock available components) \n" +
            "\n3.unlock <componentName> (to unlock components which are locked by you) \n" +
            "\n4.list (to see the list of available components)";

    private final ConversationReferences conversationReferences;
    private final Locking locking;

    private final List<String> component;

    public JenkinsBot(ConversationReferences withReferences, Locking withLocking) {
        conversationReferences = withReferences;
        locking = withLocking;
        this.component = new ArrayList<>();
    }

    /**
     * override the onMessageActivity, taking commands related to the user and share information related to the locking
     * @param turnContext
     * @return
     */
    @Override
    protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
        // save conversation reference for further proactive messaging
        addConversationReference(turnContext.getActivity());

        String[] text = turnContext.getActivity().getText().toLowerCase().split(" ");
        List<String> words = Arrays.asList(text);

        if(words.size() == 2){
            // if the command contains add as the first argument then first check the list if it is already present then notify user or create that component
            if(words.get(0).equals("add")){
                // finding index of the component
                int index = component.indexOf(words.get(1));

                // if it is already present then notify user about this
                if(index != -1){
                    return turnContext
                            .sendActivity(MessageFactory.text(String.format("'%s' already present in the library", words.get(1))))
                            .thenApply(sendResult -> null);
                }
                // else create one and add in the components list
                else{
                    component.add(words.get(1));
                    return turnContext
                            .sendActivity(MessageFactory.text(String.format("'%s' added successfully", words.get(1))))
                            .thenApply(sendResult -> null);
                }
            }
            // if command contains lock as the first argument then first check the status of component and if available then lock the component
            else if(words.get(0).equals("lock")){
                // find the index of the component
                int index = component.indexOf(words.get(1));
                // if the component is not present in the list
                if(index == -1){
                    return turnContext
                            .sendActivity(MessageFactory.text(String.format("'%s' not found in the components. \n" + "\nList of available components: \n" + getString(component), words.get(1))))
                            .thenApply(sendResult -> null);
                }
                else{
                    // fetching the userInfo of the user who previously locked the same component
                    UserInfo userInfo = locking.get(words.get(1));
                    // if it is null then locked the component with this user
                    if(userInfo == null){
                        locking.put(words.get(1), new UserInfo(turnContext.getActivity().getConversationReference().getUser().getId(), ZonedDateTime.now(ZoneId.of("UTC"))));
                        return turnContext
                                .sendActivity(MessageFactory.text(String.format("'%s' locked successfully for next 4 hours.", words.get(1))))
                                .thenApply(sendResult -> null);
                    }
                    else{
                        // if it is locked by some user previously then calculate the time difference
                        long minuteDifference = ChronoUnit.MINUTES.between(userInfo.getTime(), ZonedDateTime.now(ZoneId.of("UTC")));

                        // if it is greater than 240 minutes then unlock from that user and lock
                        if(minuteDifference >= 240){
                            locking.put(words.get(1), new UserInfo(turnContext.getActivity().getConversationReference().getUser().getId(), ZonedDateTime.now(ZoneId.of("UTC"))));
                            return turnContext
                                    .sendActivity(MessageFactory.text(String.format("'%s' locked successfully for next 4 hours.", words.get(1))))
                                    .thenApply(sendResult -> null);
                        }
                        // if it is less than 240 minutes then notify user to wait
                        else{
                            long remain = 240 - minuteDifference;
                            if(userInfo.getUserId().equals(turnContext.getActivity().getConversationReference().getUser().getId())){
                                return turnContext
                                        .sendActivity(MessageFactory.text(String.format("'%s' is already locked by you.\n \n It will be available after " + remain + " minutes for all.", words.get(1))))
                                        .thenApply(sendResult -> null);
                            }
                            return turnContext
                                    .sendActivity(MessageFactory.text(String.format("'%s' will be available after " + remain + " minutes.", words.get(1))))
                                    .thenApply(sendResult -> null);
                        }
                    }
                }
            }
            // if command is unlock then check first that the component is present and if present then check it is locked by same user or not and then notify accordingly
            else if(words.get(0).equals("unlock")){
                // find component index
                int index = component.indexOf(words.get(1));

                // if component not found
                if(index == -1){
                    return turnContext
                            .sendActivity(MessageFactory.text(String.format("'%s' cannot found in the components. \n" + "\nList of available components \n" + getString(component), words.get(1))))
                            .thenApply(sendResult -> null);
                }
                else{
                    // fetching the userInfo of the user who previously locked the same component
                    UserInfo userInfo =  locking.get(words.get(1));

                    // if it is available
                    if(userInfo == null){
                        return turnContext
                                .sendActivity(MessageFactory.text(String.format("'%s' is available.", words.get(1))))
                                .thenApply(sendResult -> null);
                    }
                    else{
                        // if it is locked by some user previously then calculate the time difference
                        long minuteDifference = ChronoUnit.MINUTES.between(userInfo.getTime(), ZonedDateTime.now(ZoneId.of("UTC")));

                        // if greater than 240 then unlock that component
                        if(minuteDifference >= 240){
                            locking.remove(words.get(1));
                            return turnContext
                                    .sendActivity(MessageFactory.text(String.format("'%s' is available.", words.get(1))))
                                    .thenApply(sendResult -> null);
                        }
                        // if same user is trying to unlock
                        else if(userInfo.getUserId().equals(turnContext.getActivity().getConversationReference().getUser().getId())){
                            locking.remove(words.get(1));
                            return turnContext
                                    .sendActivity(MessageFactory.text(String.format("'%s' unlocked successfully", words.get(1))))
                                    .thenApply(sendResult -> null);
                        }
                        // if different user is trying to unlock
                        else{
                            return turnContext
                                    .sendActivity(MessageFactory.text(String.format("You are not allowed to unlock '%s'. \n" + "\nLocked by " + userInfo.getUserId() , words.get(1))))
                                    .thenApply(sendResult -> null);
                        }
                    }
                }
            }
        }
        // if user wants to know the list of components
        else if(words.size() == 1 && words.get(0).equals("list")){
            return turnContext
                    .sendActivity(MessageFactory.text("List of available components: \n" + getString(component)))
                    .thenApply(sendResult -> null);
        }
        // notify to use the available command
        return turnContext
                .sendActivity(MessageFactory.text(notify))
                .thenApply(sendResult -> null);
    }

    /**
     * create available components list as string
     * @param component
     * @return
     */
    private String getString(List<String> component){
        if(component.isEmpty()){
            return "\nComponents list is empty (add some component using the specified command)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < component.size(); i++) {
            sb.append((i + 1)).append(". ").append(component.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * override onMembersAdded method and greet member
     * @param membersAdded
     * @param turnContext
     * @return
     */
    @Override
    protected CompletableFuture<Void> onMembersAdded(
        List<ChannelAccount> membersAdded,
        TurnContext turnContext
    ) {
        return membersAdded.stream()
            .filter(
                // Greet anyone that was not the target (recipient) of this message.
                member -> !StringUtils
                    .equals(member.getId(), turnContext.getActivity().getRecipient().getId())
            )
            .map(
                channel -> turnContext
                    .sendActivity(MessageFactory.text(String.format(welcomeMessage, port)))
            )
            .collect(CompletableFutures.toFutureList())
            .thenApply(resourceResponses -> null);
    }

    @Override
    protected CompletableFuture<Void> onConversationUpdateActivity(TurnContext turnContext) {
        addConversationReference(turnContext.getActivity());
        return super.onConversationUpdateActivity(turnContext);
    }

    // adds a ConversationReference to the shared Map.
    private void addConversationReference(Activity activity) {
        ConversationReference conversationReference = activity.getConversationReference();
        conversationReferences.put(conversationReference.getUser().getId(), conversationReference);
    }

}
