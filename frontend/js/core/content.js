/**
 * SlopeGuard — Content & Safety Copy (content.js)
 * Immutable strings, safety disclaimers, risk definitions and state labels
 */

window.LW = window.LW || {};

window.LW.content = {
  // Safety Disclaimers
  safetyDisclaimer: "Risk estimates are dependent on telemetry availability and computational hazard models. They are not guaranteed predictions and do not replace official emergency evacuation instructions from district disaster management authorities.",
  unmonitoredNote: "Areas outside the monitored regions are not assessed. The absence of a marker does not mean an area is safe.",
  cachedDataWarning: "Historical assessment displayed from local storage. Network sync required for live risk telemetry.",
  locationDenied: "Location access is unavailable. Search for a location manually to continue.",
  locationNoRegion: "No risk assessment is currently available for this location.",

  // State Messages
  loadingRegions: "Loading monitored slope regions...",
  noRegionsAvailable: "No monitored regions are currently available.",
  noSearchResults: "No matching locations found. Try another district, taluk or village.",
  mapLoadFailed: "The map could not be loaded. Please check your connection or switch to List View.",
  dataFailed: "Risk information is temporarily unavailable.",

  // Risk Level Definitions
  riskLevels: {
    low: {
      name: "Low",
      badgeClass: "lw-risk--low",
      shape: "circle",
      definition: "Normal slope equilibrium. Rainfall accumulation below cautionary saturation thresholds."
    },
    moderate: {
      name: "Moderate",
      badgeClass: "lw-risk--moderate",
      shape: "triangle",
      definition: "Elevated soil moisture detected. Heightened awareness recommended during sustained rainfall."
    },
    high: {
      name: "High",
      badgeClass: "lw-risk--high",
      shape: "square",
      definition: "Dangerous pore-pressure saturation. High probability of localized slope instability or debris flow."
    },
    critical: {
      name: "Critical",
      badgeClass: "lw-risk--critical",
      shape: "octagon",
      definition: "Imminent landslide hazard. Mandatory evacuation advisories and active emergency alerts."
    },
    none: {
      name: "Not Assessed",
      badgeClass: "lw-risk--none",
      shape: "hollow-circle",
      definition: "No recent sensor telemetry or valid computational risk model available for this sector."
    }
  },

  // Source Types
  sourceTypes: {
    ai_estimate: {
      name: "AI Risk Estimate",
      badgeClass: "lw-source--ai",
      desc: "Telemetry-driven machine learning inference, non-reviewed."
    },
    reviewed_platform: {
      name: "Reviewed Platform Alert",
      badgeClass: "lw-source--reviewed",
      desc: "Validated and published by an authorized geotechnical officer."
    },
    official_advisory: {
      name: "Official Government Advisory",
      badgeClass: "lw-source--official",
      desc: "Authorized alert issued by State Disaster Management Authority (SDMA)."
    }
  }
};
