/**
 * Action type definitions, cost resolution, and drain pipeline
 * for the Stamina module.
 *
 * <p>All stamina drains go through {@link StaminaDrainPipeline}.
 * Never write to StaminaData directly from event handlers.</p>
 */
@ApiStatus.Internal
package dev.maire.nourished.modules.Stamina.Action;

import dev.marie.MariesLib.api.ApiStatus;
