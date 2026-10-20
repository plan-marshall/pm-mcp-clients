/*
 * Copyright © 2026-present Oliver Wolff
 *
 * SPDX-License-Identifier: LicenseRef-Proprietary
 *
 * All rights reserved. This file is part of plan-marshall-mcp, which is proprietary software.
 * No right to use, copy, modify or distribute this file is granted; see the LICENSE.md file at
 * the root of this repository.
 */
package de.planmarshall.operator.planview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

import de.planmarshall.api.message.MessageCatalogue;
import de.planmarshall.api.process.LeafJob;
import de.planmarshall.api.process.LeafTask;
import de.planmarshall.api.process.NodeState;
import de.planmarshall.api.process.ProcessNode;
import de.planmarshall.api.process.ProcessTree;
import de.planmarshall.api.report.TokenFormat;
import de.planmarshall.operator.tui.TuiKeyAction;
import de.planmarshall.operator.tui.TuiKeyRegistry;
import de.planmarshall.operator.tui.TuiLayout;
import de.planmarshall.operator.tui.TuiLevel;
import de.planmarshall.operator.tui.TuiRow;

/**
 * Level 3 of the full-screen view: one plan's process as a tree that opens and closes
 * ({@code doc/design/terminal-client/16-plan-view.adoc#full-screen-tree}).
 *
 * <p>The tree opens down to the plan's current position and keeps the selection anchored on its
 * node id while the runtime advances. Every text of the runtime is sanitized here, before it
 * becomes a row: the renderer draws cells and never interprets what the runtime sent.
 */
public final class PlanViewLevel {

    /** Prefix of the row id of a process node. */
    public static final String NODE_PREFIX = "node:";
    /** Prefix of the row id of a leaf task. */
    public static final String TASK_PREFIX = "task:";
    /** The row id of the plan's line. */
    public static final String SUMMARY_ROW_ID = "summary:plan";
    /** The row id of the tokens-so-far line. */
    public static final String TOKENS_ROW_ID = "summary:tokens";

    private static final String DETAIL_PREFIX = "detail:";
    private static final String TOKENS_PREFIX = "tokens:";
    private static final String EXPANDED = "\u25be ";
    private static final String COLLAPSED = "\u25b8 ";
    private static final String NO_CHILDREN = "  ";
    private static final String DETAIL_INDENT = "    ";
    private static final String CHILD_INDENT = "  ";

    /**
     * The facts of an ended plan that the process resource does not carry.
     *
     * @param outcome the terminal outcome ({@code merged}, {@code cancelled}, ...)
     * @param reason  the reason given for the end, or {@code null}
     * @param when    the rendered time of the end, or {@code null}
     */
    public record EndedInfo(String outcome, @Nullable String reason, @Nullable String when) {
        /**
         * @param outcome the terminal outcome
         * @param reason  the reason, or {@code null}
         * @param when    the rendered time, or {@code null}
         */
        public EndedInfo {
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    private ProcessTree tree;
    private final @Nullable EndedInfo ended;
    private final Set<String> expanded = new HashSet<>();
    private final Map<String, ProcessNode> nodesById = new LinkedHashMap<>();
    private final Map<String, LeafTask> tasksById = new LinkedHashMap<>();
    private final Map<String, String> parentNodeByNode = new LinkedHashMap<>();
    private final Map<String, String> ownerNodeByTask = new LinkedHashMap<>();
    private boolean showNonLane = true;
    private boolean tokensVisible;

    /**
     * @param tree  the plan's process
     * @param ended the ended plan's facts, or {@code null} while the plan runs
     */
    public PlanViewLevel(ProcessTree tree, @Nullable EndedInfo ended) {
        this.tree = Objects.requireNonNull(tree, "tree");
        this.ended = ended;
        indexTree();
        expandCurrentPath();
    }

    /**
     * @param tree the plan's process
     */
    public PlanViewLevel(ProcessTree tree) {
        this(tree, null);
    }

    /** @return the plan's process as it was last read */
    public ProcessTree tree() {
        return tree;
    }

    /** @return the plan id */
    public String planId() {
        return tree.planId();
    }

    /** @return the project id */
    public String projectId() {
        return tree.project().projectId();
    }

    /** @return the epic id, or {@code null} when the plan belongs to no epic */
    public @Nullable String epicId() {
        return tree.epic() != null ? tree.epic().epicId() : null;
    }

    /** @return {@code true} when the plan has ended */
    public boolean isEnded() {
        return ended != null;
    }

    /** @return {@code true} while the token breakdown is shown */
    public boolean tokensVisible() {
        return tokensVisible;
    }

    /** @return {@code true} while the nodes not in the plan's lane are shown */
    public boolean showNonLane() {
        return showNonLane;
    }

    /** @return the row the view opens on: the plan's current position, else its first running node */
    public String initialRowId() {
        return activeRowId();
    }

    /**
     * Replaces the process after a live update, keeping the expansion and the selection anchor.
     *
     * @param fresh the fresh process
     */
    public void update(ProcessTree fresh) {
        this.tree = Objects.requireNonNull(fresh, "fresh");
        indexTree();
        expandCurrentPath();
    }

    /**
     * @param locale the interface locale
     * @param selectedRowId the row the selection is on, or {@code null}
     * @return the header path from the project through the plan to the selected node
     */
    public String breadcrumb(String locale, @Nullable String selectedRowId) {
        var project = PlanViewFormatting.clean(tree.project().displayName());
        var node = selectedLabel(selectedRowId, locale);
        var params = new LinkedHashMap<String, Object>();
        params.put("project", project);
        params.put("plan", PlanViewFormatting.clean(tree.planId()));
        params.put("node", node);
        if (tree.epic() != null) {
            params.put("epic", PlanViewFormatting.clean(tree.epic().epicId()));
            return MessageCatalogue.format(locale, "operator.plan.header.node_epic", params);
        }
        return MessageCatalogue.format(locale, "operator.plan.header.node", params);
    }

    /**
     * @param locale the interface locale
     * @return the footer of the plan view, with {@code e} only for a plan of an epic and {@code c}
     *         only for an ended plan
     */
    public String footer(String locale) {
        var parts = new ArrayList<String>();
        parts.add("\u2191\u2193 " + TuiKeyRegistry.label(TuiKeyAction.MOVE_UP, TuiLevel.Kind.PLAN, locale, 0));
        parts.add("Enter " + MessageCatalogue.format(locale, "operator.plan.keys.details", Map.of()));
        parts.add("\u2192 " + MessageCatalogue.format(locale, "operator.plan.keys.open", Map.of()));
        parts.add("\u2190 " + MessageCatalogue.format(locale, "operator.plan.keys.close", Map.of()));
        parts.add(". " + MessageCatalogue.format(locale, "operator.plan.keys.where", Map.of()));
        parts.add("* " + MessageCatalogue.format(locale, "operator.plan.keys.all", Map.of()));
        parts.add("n " + MessageCatalogue.format(locale, "operator.plan.keys.not_in_plan", Map.of()));
        parts.add("t " + MessageCatalogue.format(locale, "operator.plan.keys.tokens", Map.of()));
        parts.add("p " + MessageCatalogue.format(locale, "operator.plan.keys.project", Map.of()));
        if (tree.epic() != null) {
            parts.add("e " + MessageCatalogue.format(locale, "operator.plan.keys.epic", Map.of()));
        }
        if (ended != null) {
            parts.add("c " + MessageCatalogue.format(locale, "operator.plan.keys.completion", Map.of()));
        }
        parts.add("a " + MessageCatalogue.format(locale, "operator.plan.keys.actions", Map.of()));
        parts.add("Esc " + MessageCatalogue.format(locale, "operator.overview.keys.back", Map.of()));
        parts.add("q " + MessageCatalogue.format(locale, "operator.overview.keys.leave", Map.of()));
        return String.join("   ", parts);
    }

    /**
     * @param locale the interface locale
     * @return the rows of the plan view, the plan's lines first and then the tree
     */
    public List<TuiRow> rows(String locale) {
        var rows = new ArrayList<TuiRow>();
        rows.add(summaryRow(locale));
        rows.add(tokensRow(locale));
        if (tokensVisible) {
            appendTokenRows(rows, locale);
        }
        if (tree.nodes().isEmpty()) {
            rows.add(TuiRow.leaf("plan:empty",
                                MessageCatalogue.format(locale, "plan.view.load_failed", Map.of())));
            return rows;
        }
        for (var node : tree.nodes()) {
            appendNode(rows, node, 0, locale);
        }
        return rows;
    }

    /**
     * Opens the selected node one level or, on a leaf, shows its tasks.
     *
     * @param rowId the selected row
     * @return {@code true} when the tree changed
     */
    public boolean openRow(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(NODE_PREFIX)) {
            return false;
        }
        var node = nodesById.get(rowId.substring(NODE_PREFIX.length()));
        if (node == null || !hasChildren(node)) {
            return false;
        }
        if (expanded.contains(node.nodeId())) {
            expanded.remove(node.nodeId());
        } else {
            expanded.add(node.nodeId());
        }
        return true;
    }

    /**
     * Closes the selected node or, when it is closed already, moves the selection to its parent.
     *
     * @param rowId the selected row
     * @return the row to select when the node was closed and its parent is known, else {@code null}
     */
    public @Nullable String closeRow(@Nullable String rowId) {
        if (rowId == null) {
            return null;
        }
        if (rowId.startsWith(NODE_PREFIX)) {
            var nodeId = rowId.substring(NODE_PREFIX.length());
            if (expanded.remove(nodeId)) {
                return null;
            }
            var parent = parentNodeByNode.get(nodeId);
            return parent != null ? NODE_PREFIX + parent : null;
        }
        if (rowId.startsWith(TASK_PREFIX)) {
            var owner = ownerNodeByTask.get(rowId.substring(TASK_PREFIX.length()));
            return owner != null ? NODE_PREFIX + owner : null;
        }
        return null;
    }

    /**
     * Opens every level below the selected node; without a selected node it opens the whole tree.
     *
     * @param rowId the selected row, or {@code null}
     */
    public void expandAll(@Nullable String rowId) {
        if (rowId != null && rowId.startsWith(NODE_PREFIX)) {
            var node = nodesById.get(rowId.substring(NODE_PREFIX.length()));
            if (node != null) {
                expandRecursively(node);
                return;
            }
        }
        for (var node : tree.nodes()) {
            expandRecursively(node);
        }
    }

    /**
     * Expands the path to the plan's current position and returns the row of the active node.
     *
     * @return the row to select
     */
    public String jumpToActive() {
        expandCurrentPath();
        return activeRowId();
    }

    /** Shows or hides the nodes not in the plan's lane. */
    public void toggleNonLane() {
        showNonLane = !showNonLane;
    }

    /** Shows or hides the token breakdown by phase and by role. */
    public void toggleTokens() {
        tokensVisible = !tokensVisible;
    }

    /**
     * @param rowId the row
     * @return the task id when the row is a task, else {@code null}
     */
    public @Nullable String taskIdOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(TASK_PREFIX)) {
            return null;
        }
        var taskId = rowId.substring(TASK_PREFIX.length());
        return tasksById.containsKey(taskId) ? taskId : null;
    }

    /**
     * @param taskId the task id
     * @return the id of the leaf node that owns the task, or {@code null}
     */
    public @Nullable String nodeIdOfTask(String taskId) {
        return ownerNodeByTask.get(taskId);
    }

    /**
     * @param rowId the row
     * @return the plan id when the node waits for the operator, else {@code null}
     */
    public @Nullable String waitingPlanOf(@Nullable String rowId) {
        if (rowId == null || !rowId.startsWith(NODE_PREFIX)) {
            return null;
        }
        var node = nodesById.get(rowId.substring(NODE_PREFIX.length()));
        if (node == null) {
            return null;
        }
        var state = NodeState.fromValue(node.state());
        boolean waits = state == NodeState.WAITING_FOR_OPERATOR
                            || (node.waitsFor() != null && !node.waitsFor().isBlank());
        return waits ? tree.planId() : null;
    }

    /**
     * @param rowId the row
     * @return the selected node's label for the header, or the plan id
     */
    private String selectedLabel(@Nullable String rowId, String locale) {
        if (rowId != null && rowId.startsWith(NODE_PREFIX)) {
            var node = nodesById.get(rowId.substring(NODE_PREFIX.length()));
            if (node != null) {
                return nodeTitle(node, locale);
            }
        }
        if (rowId != null && rowId.startsWith(TASK_PREFIX)) {
            return PlanViewFormatting.clean(rowId.substring(TASK_PREFIX.length()));
        }
        return PlanViewFormatting.clean(tree.planId());
    }

    private void indexTree() {
        nodesById.clear();
        tasksById.clear();
        parentNodeByNode.clear();
        ownerNodeByTask.clear();
        for (var node : tree.nodes()) {
            indexNode(node, null);
        }
    }

    private void indexNode(ProcessNode node, @Nullable String parentId) {
        nodesById.put(node.nodeId(), node);
        if (parentId != null) {
            parentNodeByNode.put(node.nodeId(), parentId);
        }
        for (var task : node.leafTasks()) {
            tasksById.put(task.taskId(), task);
            ownerNodeByTask.put(task.taskId(), node.nodeId());
        }
        for (var sub : node.subNodes()) {
            indexNode(sub, node.nodeId());
        }
    }

    private void expandCurrentPath() {
        var path = tree.currentPositionPath();
        for (var id : path) {
            if (nodesById.containsKey(id)) {
                expanded.add(id);
            } else if (tasksById.containsKey(id)) {
                var owner = ownerNodeByTask.get(id);
                if (owner != null) {
                    expanded.add(owner);
                }
            }
        }
        if (!path.isEmpty()) {
            return;
        }
        for (var node : tree.nodes()) {
            if (NodeState.fromValue(node.state()) == NodeState.RUNNING) {
                expanded.add(node.nodeId());
            }
        }
    }

    private String activeRowId() {
        var path = tree.currentPositionPath();
        for (int index = path.size() - 1; index >= 0; index--) {
            var id = path.get(index);
            if (tasksById.containsKey(id)) {
                return TASK_PREFIX + id;
            }
            if (nodesById.containsKey(id)) {
                return NODE_PREFIX + id;
            }
        }
        for (var node : tree.nodes()) {
            if (NodeState.fromValue(node.state()) == NodeState.RUNNING) {
                return NODE_PREFIX + node.nodeId();
            }
        }
        return tree.nodes().isEmpty() ? SUMMARY_ROW_ID : NODE_PREFIX + tree.nodes().getFirst().nodeId();
    }

    private void expandRecursively(ProcessNode node) {
        if (!hasChildren(node)) {
            return;
        }
        expanded.add(node.nodeId());
        for (var sub : node.subNodes()) {
            expandRecursively(sub);
        }
    }

    private TuiRow summaryRow(String locale) {
        var state = MessageCatalogue.format(locale, "plan.view.state", Map.of(
                            "phase", PlanViewFormatting.clean(tree.currentPhase()),
                            "state", PlanViewFormatting.phaseState(locale, tree.phaseState())));
        var parts = new ArrayList<String>();
        parts.add(PlanViewFormatting.clean(tree.planId()));
        parts.add(state);
        var duration = PlanViewFormatting.duration(tree.durationSeconds());
        if (!duration.isEmpty()) {
            parts.add(duration);
        }
        if (tree.pullRequestNumber() != null) {
            parts.add(MessageCatalogue.format(locale, "plan.view.pull_request",
                                Map.of("number", tree.pullRequestNumber())));
        }
        if (tree.needsOperator()) {
            parts.add(MessageCatalogue.format(locale, "overview.needs.waits_for_you", Map.of()));
        }
        if (ended != null) {
            var endedParts = new ArrayList<String>();
            endedParts.add(PlanViewFormatting.clean(ended.outcome()));
            if (ended.when() != null && !ended.when().isBlank()) {
                endedParts.add(PlanViewFormatting.clean(ended.when()));
            }
            if (ended.reason() != null && !ended.reason().isBlank()) {
                endedParts.add(PlanViewFormatting.clean(ended.reason()));
            }
            parts.add(String.join(PlanViewFormatting.DOT_SEPARATOR, endedParts));
        }
        return TuiRow.leaf(SUMMARY_ROW_ID, String.join(PlanViewFormatting.DOT_SEPARATOR, parts));
    }

    private TuiRow tokensRow(String locale) {
        return TuiRow.leaf(TOKENS_ROW_ID, MessageCatalogue.format(locale, "plan.view.tokens", Map.of())
                            + "   " + PlanViewFormatting.tokens(locale, tree.tokensSoFar()));
    }

    private void appendTokenRows(List<TuiRow> rows, String locale) {
        rows.add(TuiRow.leaf(TOKENS_PREFIX + "title",
                            MessageCatalogue.format(locale, "plan.view.tokens_detail", Map.of())));
        var byPhase = new LinkedHashMap<String, Long>();
        for (var node : tree.nodes()) {
            long tokens = sumTokens(node);
            if (tokens > 0) {
                byPhase.put(nodeTitle(node, locale), tokens);
            }
        }
        rows.add(TuiRow.leaf(TOKENS_PREFIX + "by_phase_header",
                            MessageCatalogue.format(locale, "operator.plan.tokens.by_phase", Map.of())));
        for (var entry : byPhase.entrySet()) {
            rows.add(TuiRow.leaf(TOKENS_PREFIX + "phase:" + entry.getKey(),
                                DETAIL_INDENT + entry.getKey() + "  "
                                                    + TokenFormat.formatTokens(entry.getValue(), tree.tokensSoFar().floor(), locale)));
        }
        var byRole = new LinkedHashMap<String, Long>();
        for (var task : tasksById.values()) {
            byRole.merge(PlanViewFormatting.clean(task.role()), task.tokensSoFar(), Long::sum);
        }
        rows.add(TuiRow.leaf(TOKENS_PREFIX + "by_role_header",
                            MessageCatalogue.format(locale, "operator.plan.tokens.by_role", Map.of())));
        byRole.entrySet().stream()
                            .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                            .forEach(entry -> rows.add(TuiRow.leaf(TOKENS_PREFIX + "role:" + entry.getKey(),
                                                DETAIL_INDENT + entry.getKey() + "  "
                                                                    + TokenFormat.formatTokens(entry.getValue(),
                                                                    tree.tokensSoFar().floor(), locale))));
        var tokens = tree.tokensSoFar();
        var unmeasured = Math.max(0, tokens.tasksTotal() - tokens.tasksMeasured());
        rows.add(TuiRow.leaf(TOKENS_PREFIX + "coverage",
                            MessageCatalogue.format(locale, "operator.plan.tokens.coverage", Map.of(
                                                "unmeasured", unmeasured,
                                                "total", tokens.tasksTotal()))));
        rows.add(TuiRow.leaf(TOKENS_PREFIX + "components",
                            MessageCatalogue.format(locale, "operator.plan.tokens.components", Map.of(
                                                "input", TokenFormat.formatTokens(tokens.input(), tokens.floor(), locale),
                                                "output", TokenFormat.formatTokens(tokens.output(), tokens.floor(), locale)))));
    }

    private long sumTokens(ProcessNode node) {
        long total = 0;
        for (var task : node.leafTasks()) {
            total += task.tokensSoFar();
        }
        for (var sub : node.subNodes()) {
            total += sumTokens(sub);
        }
        return total;
    }

    private void appendNode(List<TuiRow> rows, ProcessNode node, int depth, String locale) {
        var state = NodeState.fromValue(node.state());
        if (state == NodeState.NOT_IN_PLAN) {
            if (showNonLane) {
                rows.add(TuiRow.leaf(NODE_PREFIX + node.nodeId(), notInPlanLine(node, depth, locale)));
            }
            return;
        }
        boolean children = hasChildren(node);
        boolean open = expanded.contains(node.nodeId());
        var line = nodeLine(node, depth, children, open, locale);
        rows.add(new TuiRow(NODE_PREFIX + node.nodeId(), line, null,
                            state == NodeState.WAITING_FOR_OPERATOR, children));
        if (!open) {
            return;
        }
        var childIndent = CHILD_INDENT.repeat(depth + 1);
        if (!node.leafTasks().isEmpty()) {
            long done = node.leafTasks().stream()
                                .filter(task -> "done".equalsIgnoreCase(task.status())
                                                    || "submitted".equalsIgnoreCase(task.status()))
                                .count();
            rows.add(TuiRow.leaf(DETAIL_PREFIX + node.nodeId() + ":tasks",
                                childIndent + MessageCatalogue.format(locale, "plan.leaf.tasks_done",
                                                    Map.of("done", done, "total", node.leafTasks().size()))));
            for (var task : node.leafTasks()) {
                rows.add(TuiRow.leaf(TASK_PREFIX + task.taskId(),
                                    childIndent + PlanViewFormatting.withGlyph(taskGlyph(task.status()),
                                                        PlanViewFormatting.taskLine(locale, task))));
            }
        }
        var job = node.leafJob();
        if (job != null) {
            rows.add(TuiRow.leaf(DETAIL_PREFIX + node.nodeId() + ":job",
                                childIndent + PlanViewFormatting.withGlyph(jobGlyph(job), jobLine(locale, job))));
        }
        for (var sub : node.subNodes()) {
            appendNode(rows, sub, depth + 1, locale);
        }
    }

    private String nodeLine(ProcessNode node, int depth, boolean children, boolean open, String locale) {
        var state = NodeState.fromValue(node.state());
        var indent = CHILD_INDENT.repeat(depth);
        var expand = children ? (open ? EXPANDED : COLLAPSED) : NO_CHILDREN;
        var stateLabel = PlanViewFormatting.nodeState(locale, state, node.runCount(), node.currentRun());
        var title = nodeTitle(node, locale);
        var info = nodeInfo(node, locale);
        var base = indent + expand + state.glyph() + " " + TuiLayout.pad(stateLabel, 16)
                            + " " + TuiLayout.pad(title, 30);
        return info.isEmpty() ? base.stripTrailing() : base + " " + info;
    }

    private String notInPlanLine(ProcessNode node, int depth, String locale) {
        var reason = node.pruneReason() != null && !node.pruneReason().isBlank()
                            ? " (" + PlanViewFormatting.clean(node.pruneReason()).replace('_', ' ') + ")"
                            : "";
        return CHILD_INDENT.repeat(depth) + NodeState.NOT_IN_PLAN.glyph() + " "
                            + MessageCatalogue.format(locale, "plan.node.state.not_in_plan", Map.of())
                            + ": " + stripKindTitle(node) + reason;
    }

    private String nodeInfo(ProcessNode node, String locale) {
        var parts = new ArrayList<String>();
        var duration = PlanViewFormatting.duration(node.durationSeconds());
        if (!duration.isEmpty()) {
            parts.add(duration);
        }
        if (!node.leafTasks().isEmpty()) {
            long done = node.leafTasks().stream()
                                .filter(task -> "done".equalsIgnoreCase(task.status())
                                                    || "submitted".equalsIgnoreCase(task.status()))
                                .count();
            parts.add(MessageCatalogue.format(locale, "plan.leaf.tasks_done",
                                Map.of("done", done, "total", node.leafTasks().size())));
        }
        if (node.waitsFor() != null && !node.waitsFor().isBlank()) {
            parts.add(PlanViewFormatting.clean(node.waitsFor()));
        }
        return String.join(PlanViewFormatting.DOT_SEPARATOR, parts);
    }

    private static String jobLine(String locale, LeafJob job) {
        var outcome = job.outcome() != null ? job.outcome() : "-";
        return MessageCatalogue.format(locale, "plan.leaf.job", Map.of(
                            "kind", PlanViewFormatting.clean(job.kind()),
                            "status", PlanViewFormatting.clean(job.status()),
                            "duration", PlanViewFormatting.duration(job.durationSeconds()),
                            "outcome", PlanViewFormatting.clean(outcome)));
    }

    private static String jobGlyph(LeafJob job) {
        return taskGlyph(job.status());
    }

    private static String taskGlyph(@Nullable String status) {
        if (status == null) {
            return NodeState.PLANNED.glyph();
        }
        return switch (status.toLowerCase(Locale.ROOT)) {
            case "done", "submitted" -> NodeState.DONE.glyph();
            case "running" -> NodeState.RUNNING.glyph();
            case "waiting", "waiting_for_you" -> NodeState.WAITING_FOR_OPERATOR.glyph();
            case "failed" -> NodeState.FAILED.glyph();
            default -> NodeState.PLANNED.glyph();
        };
    }

    private static String nodeTitle(ProcessNode node, String locale) {
        var title = PlanViewFormatting.clean(node.title());
        if ("phase".equals(node.kind())) {
            title = stripPrefix(title, "phase");
            return MessageCatalogue.format(locale, "plan.node.kind.phase", Map.of()) + " " + title;
        }
        if ("sub_workflow".equals(node.kind())) {
            title = stripPrefix(title, "sub-workflow");
            return "\u21b3 " + MessageCatalogue.format(locale, "plan.node.kind.sub_workflow", Map.of()) + " " + title;
        }
        return title;
    }

    private static String stripKindTitle(ProcessNode node) {
        var title = PlanViewFormatting.clean(node.title());
        if ("phase".equals(node.kind())) {
            return stripPrefix(title, "phase");
        }
        if ("sub_workflow".equals(node.kind())) {
            return stripPrefix(title, "sub-workflow");
        }
        return title;
    }

    private static String stripPrefix(String title, String prefix) {
        return title.regionMatches(true, 0, prefix, 0, prefix.length())
                            ? title.substring(prefix.length()).strip()
                            : title;
    }

    private static boolean hasChildren(ProcessNode node) {
        return !node.subNodes().isEmpty() || !node.leafTasks().isEmpty() || node.leafJob() != null;
    }

}
