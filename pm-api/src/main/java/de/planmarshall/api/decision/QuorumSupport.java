/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.api.decision;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.json.JsonTree;

/**
 * Quorum voting support metrics and votes for a decision option (UC-PLN-2).
 *
 * @param votesCount     number of votes in favour
 * @param totalVotes     total votes cast in the quorum
 * @param modelsCount    number of distinct models voting in favour
 * @param band           overall quorum consensus band
 * @param dissentSummary strongest dissenting argument if any
 * @param votes          individual vote breakdowns
 */
public record QuorumSupport(
        int votesCount,
        int totalVotes,
        int modelsCount,
        QuorumBand band,
        @Nullable String dissentSummary,
        List<QuorumVote> votes
) {
    public QuorumSupport {
        Objects.requireNonNull(band, "band");
        votes = votes != null ? List.copyOf(votes) : List.of();
    }

    /**
     * Converts to JSON tree map.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("votes_count", votesCount);
        map.put("total_votes", totalVotes);
        map.put("models_count", modelsCount);
        map.put("band", band.value());
        if (dissentSummary != null) {
            map.put("dissent_summary", dissentSummary);
        }
        var voteList = new ArrayList<Map<String, Object>>();
        for (var v : votes) {
            voteList.add(v.toMap());
        }
        map.put("votes", voteList);
        return map;
    }

    /**
     * Parses from JSON map.
     *
     * @param map tree map
     * @return parsed QuorumSupport
     */
    public static QuorumSupport fromMap(Map<String, Object> map) {
        Objects.requireNonNull(map, "map");
        var votesCount = JsonTree.integer(map, "votes_count") != null ? JsonTree.integer(map, "votes_count") : 0;
        var totalVotes = JsonTree.integer(map, "total_votes") != null ? JsonTree.integer(map, "total_votes") : 0;
        var modelsCount = JsonTree.integer(map, "models_count") != null ? JsonTree.integer(map, "models_count") : 0;
        var bandStr = JsonTree.string(map, "band");
        var band = bandStr != null ? QuorumBand.fromValue(bandStr) : QuorumBand.SPLIT;
        var dissent = JsonTree.string(map, "dissent_summary");
        if (dissent == null) dissent = JsonTree.string(map, "dissent");

        var votesList = new ArrayList<QuorumVote>();
        if (map.get("votes") instanceof List<?> list) {
            for (var item : list) {
                var vm = JsonTree.asObject(item);
                if (vm != null) {
                    votesList.add(QuorumVote.fromMap(vm));
                }
            }
        }
        return new QuorumSupport(votesCount, totalVotes, modelsCount, band, dissent, votesList);
    }
}
