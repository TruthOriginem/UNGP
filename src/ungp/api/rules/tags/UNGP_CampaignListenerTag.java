package ungp.api.rules.tags;

/**
 * Add transient listener to campaign layer...Listeners could be found in {@link com.fs.starfarer.api.campaign.listeners}
 *
 * @param <T>
 */
public interface UNGP_CampaignListenerTag<T> {
    T getListener();

    Class<T> getClassOfListener();
}
