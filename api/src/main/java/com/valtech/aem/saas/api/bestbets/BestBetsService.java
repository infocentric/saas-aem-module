package com.valtech.aem.saas.api.bestbets;

import com.valtech.aem.saas.api.bestbets.dto.BestBetDTO;
import com.valtech.aem.saas.api.bestbets.dto.BestBetPayloadDTO;
import java.util.List;
import lombok.NonNull;

/**
 * Represents a service that manages best bets.
 */
public interface BestBetsService {

  /**
   * Adds a best bet entry in saas admin.
   *
   * @param client            identifying string.
   * @param bestBetPayloadDto object containing details for the best bet.
   * @throws IllegalArgumentException      exception thrown when blank client argument is passed
   * @throws IllegalStateException         exception thrown when according action is not specified
   * @throws BestBetsActionFailedException exception thrown when the add action has failed or request execution has
   *                                       failed
   */
  void addBestBet(@NonNull String client, @NonNull BestBetPayloadDTO bestBetPayloadDto);

  /**
   * Adds a list of best bet entry in saas admin.
   *
   * @param client                identifying string.
   * @param bestBetPayloadDTOList a list of objects containing best bet details.
   * @throws IllegalArgumentException      exception thrown when blank client argument is passed
   * @throws IllegalStateException         exception thrown when according action is not specified
   * @throws BestBetsActionFailedException exception thrown when the add action has failed or request execution has
   *                                       failed
   */
  void addBestBets(@NonNull String client, @NonNull List<BestBetPayloadDTO> bestBetPayloadDTOList);

  /**
   * Updates the best bet entry with the specified id.
   *
   * @param client            identifying string.
   * @param bestBetId         id of the best bet that is updated.
   * @param bestBetPayloadDto best bet details to be updated.
   * @throws IllegalArgumentException      exception thrown when blank client argument is passed
   * @throws IllegalStateException         exception thrown when according action is not specified
   * @throws BestBetsActionFailedException exception thrown when the update action has failed or request execution has
   *                                       failed
   */
  void updateBestBet(@NonNull String client, int bestBetId, @NonNull BestBetPayloadDTO bestBetPayloadDto);

  /**
   * Deletes the best bet entry with the specified id.
   *
   * @param client    identifying string.
   * @param bestBetId the id of the best bet that should be deleted.
   * @throws IllegalArgumentException      exception thrown when blank client argument is passed
   * @throws IllegalStateException         exception thrown when according action is not specified
   * @throws BestBetsActionFailedException exception thrown when the delete action has failed or request execution has
   *                                       failed.
   */
  void deleteBestBet(@NonNull String client, int bestBetId);

  /**
   * Published the best bets for the specified project.
   *
   * @param client    identifying string.
   * @param projectId id of the project whose best bets should be published
   * @throws IllegalArgumentException      exception thrown when blank client argument is passed
   * @throws IllegalStateException         exception thrown when according action is not specified
   * @throws BestBetsActionFailedException exception thrown when the publishing action has failed or request execution
   *                                       has failed
   */
  void publishBestBetsForProject(@NonNull String client, int projectId);

  /**
   * Gets the list of all best bets entries.
   *
   * @param client identifying string.
   * @return list of best bets.
   * @throws IllegalArgumentException      exception thrown when blank client argument is passed
   * @throws IllegalStateException         exception thrown when according action is not specified
   * @throws BestBetsActionFailedException exception thrown when the request execution has failed.
   */
  List<BestBetDTO> getBestBets(@NonNull String client);
}
