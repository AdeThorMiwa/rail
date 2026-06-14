package com.rail.api.service;

import com.rail.api.entity.Intention;
import com.rail.api.entity.IntentionProposal;
import com.rail.api.entity.IntentionProposalStatus;
import com.rail.api.entity.IntentionStatus;
import com.rail.api.entity.User;
import com.rail.api.intelligence.IntentionBlueprint;
import com.rail.api.repository.IntentionProposalRepository;
import com.rail.api.repository.IntentionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntentionGenerationService {

    private final GoalGenerationService goalGenerationService;
    private final IntentionRepository intentionRepository;
    private final IntentionProposalRepository proposalRepository;

    @Transactional
    public Intention generateFromProposal(
        User user,
        IntentionProposal proposal
    ) {
        IntentionBlueprint ib = proposal.getSynthesis().intention();

        Intention intention = intentionRepository.saveAndFlush(
            Intention.builder()
                .owner(user)
                .rawInput(ib.title())
                .title(ib.title())
                .type(ib.intentionType())
                .completionCriteria(ib.completionCriteria())
                .status(IntentionStatus.ACTIVE)
                .build()
        );

        goalGenerationService.generateFromBlueprint(
            intention,
            proposal.getSynthesis().goal()
        );

        proposal.setStatus(IntentionProposalStatus.CREATED);
        proposalRepository.save(proposal);

        return intention;
    }
}
