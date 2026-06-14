package com.rail.api.entity;

/**
 * Goal types are primary behavior descriptors — they tell Rail how to primarily measure progress
 * and generate tasks. They are not structural gatekeepers.
 *
 * <table>
 *   <tr>
 *     <th>Type</th>
 *     <th>Primary behavior</th>
 *     <th>Milestones</th>
 *   </tr>
 *   <tr>
 *     <td>HABIT</td>
 *     <td>Streak / frequency tracking</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>ABSTINENCE</td>
 *     <td>Avoidance streak</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>PROJECT</td>
 *     <td>Milestone progression toward a defined outcome</td>
 *     <td>Yes</td>
 *   </tr>
 *   <tr>
 *     <td>TASK</td>
 *     <td>Binary completion — simple one-off</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *     <td>QUANTIFIED</td>
 *     <td>Counter vs target (primary success measure)</td>
 *     <td>Yes</td>
 *   </tr>
 * </table>
 */

public enum GoalType {
    HABIT,
    ABSTINENCE,
    PROJECT,
    TASK,
    QUANTIFIED,
}
