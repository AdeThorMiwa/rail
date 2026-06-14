CREATE UNIQUE INDEX ux_intention_proposal_one_refining_per_user ON intention_proposals(owner_id) WHERE status = 'REFINING';
