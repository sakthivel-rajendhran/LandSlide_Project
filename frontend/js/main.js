/**
 * SlopeGuard — Main UI Interactions & Accessibility
 * Handles navigation, mobile menu, active link tracking, and scroll effects
 */

document.addEventListener('DOMContentLoaded', () => {
  initNavbarScroll();
  initLogoScrollAnimation();
  initMobileMenu();
  initNavigationAndScrollTracking();
  initScrollAnimations();
  initRiskIntelligenceMap();
});

/**
 * Global reference to close mobile menu cleanly
 */
let closeMobileMenuHandler = null;

/**
 * Header scroll state (adds .scrolled class for frosted glass on scroll)
 */
function initNavbarScroll() {
  const header = document.querySelector('.site-header');
  if (!header) return;

  let isTicking = false;
  const handleScroll = () => {
    if (window.scrollY > 20) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
    isTicking = false;
  };

  window.addEventListener('scroll', () => {
    if (!isTicking) {
      window.requestAnimationFrame(handleScroll);
      isTicking = true;
    }
  }, { passive: true });
  handleScroll(); // Initial check
}

/**
 * SlopeGuard Logo Text Scroll Animation
 * Animates ONLY the header logo text (.site-header .brand-name):
 * - Scroll upward: smoothly fades upward (subtle translateY(-8px), opacity: 0)
 * - Reach Home section: smoothly fades downward into its original position (translateY(0), opacity: 1)
 * - Scroll downward: consistent and smooth (restores / maintains original position)
 * Jitter-rejection threshold (8px) prevents flicker from micro-scrolls.
 * Throttled via requestAnimationFrame, respects prefers-reduced-motion.
 */
function initLogoScrollAnimation() {
  const logoText = document.querySelector('.site-header .brand-logo .brand-name');
  if (!logoText) return;

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)');
  if (prefersReduced.matches) return;

  let lastScrollY = Math.max(0, window.scrollY || window.pageYOffset);
  let accumulatedDelta = 0;
  let currentState = 'home'; // 'home' | 'up' | 'down'
  let isTicking = false;
  const JITTER_THRESHOLD = 8; // Pixels required to switch directions, preventing flicker

  const setLogoTextState = (state) => {
    if (currentState === state) return;
    currentState = state;

    if (state === 'up') {
      logoText.setAttribute('data-scroll-state', 'up');
      logoText.classList.add('fade-up');
      logoText.classList.remove('fade-home');
    } else {
      // 'home' or 'down'
      logoText.setAttribute('data-scroll-state', state);
      logoText.classList.remove('fade-up');
      logoText.classList.add('fade-home');
    }
  };

  const handleScroll = () => {
    const currentScrollY = Math.max(0, window.scrollY || window.pageYOffset);
    const delta = currentScrollY - lastScrollY;

    // 1. Boundary check: If user is within Home section (top 80px), restore home state
    if (currentScrollY <= 80) {
      accumulatedDelta = 0;
      setLogoTextState('home');
      lastScrollY = currentScrollY;
      return;
    }

    // 2. Ignore sub-pixel floating noise (< 1.5px)
    if (Math.abs(delta) < 1.5) {
      return;
    }

    // 3. Directional scroll detection with jitter protection
    if (delta < 0) {
      // Scrolling UP
      if (accumulatedDelta > 0) accumulatedDelta = 0;
      accumulatedDelta += delta;

      if (accumulatedDelta <= -JITTER_THRESHOLD) {
        setLogoTextState('up');
      }
    } else if (delta > 0) {
      // Scrolling DOWN
      if (accumulatedDelta < 0) accumulatedDelta = 0;
      accumulatedDelta += delta;

      if (accumulatedDelta >= JITTER_THRESHOLD) {
        setLogoTextState('down');
      }
    }

    lastScrollY = currentScrollY;
  };

  window.addEventListener('scroll', () => {
    if (!isTicking) {
      window.requestAnimationFrame(() => {
        handleScroll();
        isTicking = false;
      });
      isTicking = true;
    }
  }, { passive: true });

  // Handle Home section activation event (from scroll spy or nav click)
  window.addEventListener('slopeguard:homeactive', () => {
    accumulatedDelta = 0;
    setLogoTextState('home');
  });

  // Handle dynamic accessibility changes
  if (prefersReduced.addEventListener) {
    prefersReduced.addEventListener('change', (e) => {
      if (e.matches) {
        setLogoTextState('home');
      }
    });
  }

  // Initial check on page load
  handleScroll();
}

/**
 * Mobile Navigation Drawer & Keyboard Accessibility
 */
function initMobileMenu() {
  const toggleBtn = document.querySelector('.mobile-toggle');
  const navMenu = document.querySelector('.nav-bar') || document.querySelector('.nav-links');
  if (!toggleBtn || !navMenu) return;

  const toggleMenu = (open) => {
    const isOpen = open !== undefined ? open : !navMenu.classList.contains('nav-open');
    if (isOpen) {
      navMenu.classList.add('nav-open');
      toggleBtn.classList.add('is-active');
      toggleBtn.setAttribute('aria-expanded', 'true');
      toggleBtn.innerHTML = `
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      `;
      document.body.style.overflow = 'hidden';
    } else {
      navMenu.classList.remove('nav-open');
      toggleBtn.classList.remove('is-active');
      toggleBtn.setAttribute('aria-expanded', 'false');
      toggleBtn.innerHTML = `
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
        </svg>
      `;
      document.body.style.overflow = '';
    }
  };

  window.closeMobileMenu = () => toggleMenu(false);
  const closeMobileMenuHandler = window.closeMobileMenu;

  toggleBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    toggleMenu();
  });

  // Close on Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && navMenu.classList.contains('nav-open')) {
      toggleMenu(false);
      toggleBtn.focus();
    }
  });

  // Close when clicking outside of menu on mobile
  document.addEventListener('click', (e) => {
    if (navMenu.classList.contains('nav-open') && 
        !navMenu.contains(e.target) && 
        !toggleBtn.contains(e.target)) {
      toggleMenu(false);
    }
  });
}

/**
 * Navigation Click Behavior & Automatic Active Section Scroll Tracking
 */
function initNavigationAndScrollTracking() {
  const header = document.querySelector('.site-header');
  const navLinks = document.querySelectorAll('.nav-links .nav-link[href^="#"]');
  const trackedSections = Array.from(document.querySelectorAll('section[id]'));
  const allAnchorLinks = document.querySelectorAll('a[href^="#"]');
  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const trackedNavIds = Array.from(navLinks)
    .map(link => (link.getAttribute('href') || '').replace('#', ''))
    .filter(Boolean);
  const navigableSections = trackedSections.filter(s => trackedNavIds.includes(s.id));

  if (!trackedSections.length || !navLinks.length) return;

  let isClickScrolling = false;
  let clickScrollTimer = null;
  let isScrollTicking = false;

  /**
   * Set active state on the matching nav link and clear from others
   * Sets .active, .is-active, and aria-current="location"
   */
  function setActiveNavLink(activeId) {
    navLinks.forEach(link => {
      const href = link.getAttribute('href');
      const isMatch = href === `#${activeId}`;
      if (isMatch) {
        link.classList.add('active', 'is-active');
        link.setAttribute('aria-current', 'location');
      } else {
        link.classList.remove('active', 'is-active');
        link.removeAttribute('aria-current');
      }
    });

    if (activeId === 'home') {
      window.dispatchEvent(new CustomEvent('slopeguard:homeactive'));
    }
  }

  /**
   * Locates the primary heading element inside a destination section
   */
  function getDestinationHeading(targetEl) {
    if (!targetEl) return null;
    if (/^H[1-6]$/i.test(targetEl.tagName)) return targetEl;
    return targetEl.querySelector(
      '.features-header, .map-section-header, .workflow-header, .intro-content, .cta-content-wrapper, .section-header, .panel-header, .section-title, .section-badge, h1, h2, h3'
    ) || targetEl;
  }

  /**
   * Calculates the real bottom edge of the sticky/fixed navbar in the viewport
   */
  function getEffectiveNavbarBottom() {
    if (!header) {
      return window.innerWidth <= 960 ? 60 : 72;
    }
    const rect = header.getBoundingClientRect();
    const subContainer = header.querySelector('.header-container, .nav-bar') || header;
    const subRect = subContainer.getBoundingClientRect();
    const bottom = Math.max(rect.bottom, subRect.bottom);
    return Math.round(bottom > 0 ? bottom : (window.innerWidth <= 960 ? 60 : 72));
  }

  /**
   * Evaluates active section based on scroll position and viewport visibility
   */
  function updateActiveSectionOnScroll() {
    if (isClickScrolling) return;

    const scrollY = window.scrollY || window.pageYOffset;
    const viewportHeight = window.innerHeight;
    const docHeight = document.documentElement.scrollHeight;

    // 1. Top of page boundary: Home is active
    if (scrollY <= 60) {
      setActiveNavLink('home');
      return;
    }

    // 2. Bottom of document boundary: Last navigable section is active
    if (scrollY + viewportHeight >= docHeight - 50) {
      const lastSection = navigableSections[navigableSections.length - 1];
      if (lastSection) {
        setActiveNavLink(lastSection.id);
      }
      return;
    }

    // 3. Middle sections: Determine which section heading is currently at or below the navbar
    const navBottom = getEffectiveNavbarBottom();
    const threshold = navBottom + 36;

    let currentSectionId = 'home';
    for (let i = 0; i < navigableSections.length; i++) {
      const section = navigableSections[i];
      const heading = getDestinationHeading(section);
      const headingRect = heading.getBoundingClientRect();
      const sectionRect = section.getBoundingClientRect();
      if (headingRect.top <= threshold && sectionRect.bottom > navBottom) {
        currentSectionId = section.id;
      }
    }

    setActiveNavLink(currentSectionId);
  }

  /**
   * Calculates the exact scroll destination using actual navbar bottom and 12px visual gap (within 8-16px)
   */
  function getSectionScrollTop(targetEl) {
    if (!targetEl || targetEl.id === 'home') {
      return 0;
    }
    const headingEl = getDestinationHeading(targetEl);
    const navBottom = getEffectiveNavbarBottom();
    const headingDocTop = headingEl.getBoundingClientRect().top + (window.pageYOffset || document.documentElement.scrollTop);
    // Target heading position = navbar bottom + 12px
    const targetY = headingDocTop - navBottom - 12;
    return Math.max(0, Math.round(targetY));
  }

  /**
   * Handle completion of a click-initiated scroll
   */
  function onScrollFinished() {
    isClickScrolling = false;
    if (clickScrollTimer) {
      clearTimeout(clickScrollTimer);
      clickScrollTimer = null;
    }
    updateActiveSectionOnScroll();
  }

  /**
   * Handle in-page navigation clicks cleanly without stopping mid-section
   */
  function handleLinkClick(e) {
    const href = this.getAttribute('href');
    if (!href || !href.startsWith('#')) return;

    const targetId = href.slice(1);
    const targetEl = document.getElementById(targetId);

    // If ID does not exist on page and is not home, allow normal behavior
    if (!targetEl && targetId !== 'home') return;

    e.preventDefault();

    // 1. Immediately close mobile menu if currently open and unlock scroll
    if (typeof window.closeMobileMenu === 'function') {
      window.closeMobileMenu();
    }
    const navBar = document.querySelector('.nav-bar');
    if (navBar) navBar.classList.remove('nav-open');
    document.body.style.overflow = '';

    // 2. Immediately update active nav state without waiting for scroll
    setActiveNavLink(targetId);

    // 3. Defer scroll calculation to next animation frame so header layout reflow completes
    requestAnimationFrame(() => {
      isClickScrolling = true;
      if (clickScrollTimer) clearTimeout(clickScrollTimer);

      const targetY = (targetId === 'home' || !targetId) ? 0 : getSectionScrollTop(targetEl);

      window.scrollTo({
        top: targetY,
        behavior: prefersReduced ? 'auto' : 'smooth'
      });

      if (window.location.hash !== `#${targetId}`) {
        history.pushState(null, '', `#${targetId}`);
      }

      // Stability verification after smooth scroll completes
      const scrollDuration = prefersReduced ? 50 : 850;
      clickScrollTimer = setTimeout(() => {
        if (targetEl && targetId !== 'home') {
          const finalY = getSectionScrollTop(targetEl);
          if (Math.abs(window.pageYOffset - finalY) > 10) {
            window.scrollTo({ top: finalY, behavior: 'auto' });
          }
        }
        onScrollFinished();
      }, scrollDuration);
    });
  }

  // Support native 'scrollend' event for precision unlocking
  if ('onscrollend' in window) {
    window.addEventListener('scrollend', () => {
      if (isClickScrolling) {
        onScrollFinished();
      }
    }, { passive: true });
  }

  // Attach click handler to all in-page anchors
  allAnchorLinks.forEach(link => {
    link.addEventListener('click', handleLinkClick);
  });

  // Attach throttled rAF scroll listener
  window.addEventListener('scroll', () => {
    if (!isScrollTicking) {
      window.requestAnimationFrame(() => {
        updateActiveSectionOnScroll();
        isScrollTicking = false;
      });
      isScrollTicking = true;
    }
  }, { passive: true });

  // Handle window resize to recalculate active section
  window.addEventListener('resize', () => {
    updateActiveSectionOnScroll();
  }, { passive: true });

  // Handle initial page position, deep links, and page refresh
  function handleInitialPosition() {
    if ('scrollRestoration' in history) {
      history.scrollRestoration = 'manual';
    }

    const hash = window.location.hash;
    if (hash && hash !== '#home') {
      const targetId = hash.slice(1);
      const targetEl = document.getElementById(targetId);
      if (targetEl) {
        setActiveNavLink(targetId);
        isClickScrolling = true;
        if (clickScrollTimer) clearTimeout(clickScrollTimer);

        const targetY = getSectionScrollTop(targetEl);
        window.scrollTo({
          top: targetY,
          behavior: 'auto'
        });

        // Stabilize after dynamic assets / fonts / images settle
        setTimeout(() => {
          const finalY = getSectionScrollTop(targetEl);
          if (Math.abs(window.pageYOffset - finalY) > 10) {
            window.scrollTo({ top: finalY, behavior: 'auto' });
          }
          onScrollFinished();
        }, 250);
        return;
      }
    }

    // Fresh visit or homepage link: always land cleanly at top of page
    if (!hash || hash === '#home') {
      window.scrollTo(0, 0);
      setActiveNavLink('home');
    } else {
      updateActiveSectionOnScroll();
    }
  }

  // Execute initial placement and listen for hash changes
  handleInitialPosition();
  window.addEventListener('load', () => {
    // Re-check hash target once all high-resolution images have fully loaded
    const hash = window.location.hash;
    if (hash && hash !== '#home') {
      const targetEl = document.getElementById(hash.slice(1));
      if (targetEl) {
        const accurateY = getSectionScrollTop(targetEl);
        if (Math.abs(window.pageYOffset - accurateY) > 8) {
          window.scrollTo({ top: accurateY, behavior: 'auto' });
        }
      }
    }
  });
  window.addEventListener('hashchange', handleInitialPosition);
}

/**
 * Subtle Scroll Reveal Animations (respects prefers-reduced-motion)
 */
function initScrollAnimations() {
  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (prefersReduced) return;

  const animatedElements = document.querySelectorAll('.feature-card, .workflow-step-card, .factor-card, .intro-visual-card');
  if (!animatedElements.length) return;

  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.style.opacity = '1';
        entry.target.style.transform = 'translateY(0)';
        revealObserver.unobserve(entry.target);
      }
    });
  }, {
    threshold: 0.12,
    rootMargin: '0px 0px -40px 0px'
  });

  animatedElements.forEach((el, index) => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(24px)';
    el.style.transition = `opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1) ${index % 4 * 0.08}s, transform 0.6s cubic-bezier(0.16, 1, 0.3, 1) ${index % 4 * 0.08}s`;
    revealObserver.observe(el);
  });
}

/**
 * ============================================================================
 * Risk Intelligence — Wayanad Geographic Map & Telemetry HUD Synchronizer
 * ============================================================================
 */
function initRiskIntelligenceMap() {
  const riskSection = document.getElementById('risk-intelligence');
  if (!riskSection) return;

  const WAYANAD_SECTOR_DATA = {
    all: {
      name: "All Sectors (Wayanad)",
      coords: "11.6050° N, 76.1320° E (District Corridor)",
      riskScore: "75% High Susceptibility",
      riskClass: "alert",
      precipitation: "138.8 mm (Regional Mean)",
      saturation: "72% Mean Saturation",
      saturationClass: "alert",
      source: "GSI Susceptibility & IMD AWS Grid",
      model: "v2.4-convLSTM",
      observation: "Updated 14 min ago",
      transform: "scale(1) translate(0%, 0%)"
    },
    meppadi: {
      name: "Meppadi Ridge (Chooralmala Corridor)",
      coords: "11.5512° N, 76.1264° E • Elev: 860m",
      riskScore: "92% Severe Hotspot",
      riskClass: "severe",
      precipitation: "168.4 mm (Meppadi Station W-04)",
      saturation: "84% Critical Saturation",
      saturationClass: "severe",
      source: "Model v2.4-convLSTM • IMD Rain Gauge (W-04)",
      model: "v2.4-convLSTM",
      observation: "Updated 12 min ago",
      transform: "scale(1.28) translate(-7%, -4%)"
    },
    kalpetta: {
      name: "Kalpetta Slopes",
      coords: "11.6050° N, 76.0830° E • Elev: 780m",
      riskScore: "58% Moderate Risk",
      riskClass: "moderate",
      precipitation: "62.0 mm (Kalpetta Station)",
      saturation: "52% Moderate Saturation",
      saturationClass: "moderate",
      source: "District Hydro-met Array • Model v2.4-convLSTM",
      model: "v2.4-convLSTM",
      observation: "Updated 24 min ago",
      transform: "scale(1.25) translate(1%, 6%)"
    },
    vythiri: {
      name: "Vythiri Pass (Thamarassery Corridor)",
      coords: "11.5385° N, 76.0354° E • Elev: 710m",
      riskScore: "76% High Risk",
      riskClass: "alert",
      precipitation: "186.2 mm (Lakkidi High-Rainfall Gauge)",
      saturation: "79% Saturated",
      saturationClass: "alert",
      source: "PWD Ghat Inclinometer Array • IMD Lakkidi Gauge",
      model: "v2.4-convLSTM",
      observation: "Updated 10 min ago",
      transform: "scale(1.28) translate(13%, -2%)"
    }
  };

  const mapImg = document.getElementById('wayanadMapImg');
  const coordsDisplay = document.getElementById('mapCoordsDisplay');
  const hudSectorName = document.getElementById('hudSectorName');
  const hudRiskScore = document.getElementById('hudRiskScore');
  const hudPrecipitation = document.getElementById('hudPrecipitation');
  const hudSaturation = document.getElementById('hudSaturation');
  const hudDataSource = document.getElementById('hudDataSource');
  const hudModelEngine = document.getElementById('hudModelEngine');
  const hudObservationTime = document.getElementById('hudObservationTime');
  const hudModeBadge = document.getElementById('hudModeBadge');
  const mapCaptionText = document.getElementById('mapCaptionText');
  const btnAccess = document.getElementById('btnAccessMonitoring');
  const btnAccessText = document.getElementById('btnAccessMonitoringText');

  const sectorButtons = riskSection.querySelectorAll('.filter-pills-group [data-sector]');
  const pinMarkers = riskSection.querySelectorAll('.sector-pin-marker');

  const btnToggleHazard = document.getElementById('toggleRiskLayer');
  const btnToggleRainfall = document.getElementById('toggleRainfallLayer');
  const layerHazard = document.getElementById('layerHazardBuffers');
  const layerRainfall = document.getElementById('layerRainfall');
  const layerStatusPill = document.getElementById('mapLayerStatusPill');
  const layerStatusText = document.getElementById('mapLayerStatusText');

  let activeHazardLayer = true;
  let activeRainfallLayer = false;
  let currentSector = 'all';

  function checkUserSession() {
    let user = null;
    try {
      const raw = sessionStorage.getItem('sg_user') || localStorage.getItem('sg_user');
      if (raw) user = JSON.parse(raw);
    } catch (e) {}

    if (user) {
      if (hudModeBadge) {
        hudModeBadge.textContent = "Live Telemetry";
        hudModeBadge.classList.add("live");
      }
      if (mapCaptionText) {
        mapCaptionText.textContent = `Authorized telemetry active • Officer ${user.name || user.district || 'Command'}`;
      }
      if (btnAccess) {
        btnAccess.href = "dashboard.html";
        if (btnAccessText) btnAccessText.textContent = "Open Dedicated Map Console";
      }
    } else {
      if (hudModeBadge) {
        hudModeBadge.textContent = "Illustrative";
        hudModeBadge.classList.remove("live");
      }
      if (mapCaptionText) {
        mapCaptionText.textContent = "Illustrative risk visualization — sign in to access live map monitoring.";
      }
      if (btnAccess) {
        btnAccess.href = "login.html?next=map.html";
        if (btnAccessText) btnAccessText.textContent = "Access Live Map Monitoring";
      }
    }
  }

  function updateLayerControls() {
    if (layerHazard) layerHazard.classList.toggle('visible', activeHazardLayer);
    if (btnToggleHazard) btnToggleHazard.classList.toggle('active', activeHazardLayer);

    if (layerRainfall) layerRainfall.classList.toggle('visible', activeRainfallLayer);
    if (btnToggleRainfall) btnToggleRainfall.classList.toggle('active', activeRainfallLayer);

    if (layerStatusPill && layerStatusText) {
      if (activeHazardLayer && activeRainfallLayer) {
        layerStatusText.textContent = "Hazard Buffers & 24h Rainfall Layers Active";
        layerStatusPill.style.display = "inline-flex";
      } else if (activeHazardLayer) {
        layerStatusText.textContent = "Hazard Buffers Layer Active (GSI Envelope)";
        layerStatusPill.style.display = "inline-flex";
      } else if (activeRainfallLayer) {
        layerStatusText.textContent = "24h Isohyet Rainfall Layer Active (IMD Network)";
        layerStatusPill.style.display = "inline-flex";
      } else {
        layerStatusText.textContent = "Base Topography View (Layers Off)";
        layerStatusPill.style.display = "inline-flex";
      }
    }
  }

  function applySector(sectorKey) {
    const data = WAYANAD_SECTOR_DATA[sectorKey];
    currentSector = sectorKey;

    // 1. Update filter pill buttons
    sectorButtons.forEach(btn => {
      btn.classList.toggle('active', btn.dataset.sector === sectorKey);
    });

    // 2. Update interactive pins on map
    pinMarkers.forEach(pin => {
      pin.classList.toggle('active', pin.dataset.sector === sectorKey);
    });

    if (!data) {
      // Missing / unsupported sector state
      if (hudSectorName) hudSectorName.textContent = "Data unavailable";
      if (hudRiskScore) {
        hudRiskScore.textContent = "Data unavailable";
        hudRiskScore.className = "hud-val";
      }
      if (hudPrecipitation) hudPrecipitation.textContent = "Data unavailable";
      if (hudSaturation) {
        hudSaturation.textContent = "Data unavailable";
        hudSaturation.className = "hud-val";
      }
      if (hudDataSource) hudDataSource.textContent = "Sensor telemetry offline";
      if (hudObservationTime) hudObservationTime.textContent = "--";
      return;
    }

    // 3. Update map viewport (smooth pan & zoom)
    if (mapImg) {
      mapImg.style.transform = data.transform;
    }

    // 4. Update coordinates display in top bar
    if (coordsDisplay) {
      coordsDisplay.textContent = `Coordinates: ${data.coords}`;
    }

    // 5. Update Telemetry HUD metrics
    if (hudSectorName) hudSectorName.textContent = data.name;
    if (hudRiskScore) {
      hudRiskScore.textContent = data.riskScore;
      hudRiskScore.className = `hud-val ${data.riskClass}`;
    }
    if (hudPrecipitation) hudPrecipitation.textContent = data.precipitation;
    if (hudSaturation) {
      hudSaturation.textContent = data.saturation;
      hudSaturation.className = `hud-val ${data.saturationClass}`;
    }
    if (hudDataSource) hudDataSource.textContent = data.source;
    if (hudModelEngine) hudModelEngine.textContent = data.model;
    if (hudObservationTime) hudObservationTime.textContent = data.observation;
  }

  // Attach button event listeners
  sectorButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      applySector(btn.dataset.sector || 'all');
    });
  });

  // Attach pin event listeners
  pinMarkers.forEach(pin => {
    pin.addEventListener('click', (e) => {
      e.stopPropagation();
      applySector(pin.dataset.sector || 'all');
    });
  });

  // Layer toggles
  if (btnToggleHazard) {
    btnToggleHazard.addEventListener('click', () => {
      activeHazardLayer = !activeHazardLayer;
      updateLayerControls();
    });
  }

  if (btnToggleRainfall) {
    btnToggleRainfall.addEventListener('click', () => {
      activeRainfallLayer = !activeRainfallLayer;
      updateLayerControls();
    });
  }

  // Initialize
  checkUserSession();
  updateLayerControls();
  applySector('all');
}
