<script lang="ts">
  /**
   * The waiting mark (brand-mark.adoc, The Waiting Mark): the mascot's clipboard, whose three lines
   * are ticked one after another while the operator waits on the system, beside the text that says
   * what runs.
   *
   * The line is a polite status region, so a screen reader hears the text; the mark is decorative.
   * Each open dot and each tick is its own element, animated by this stylesheet alone: a style
   * attribute is blocked by the daemon's content security policy.
   */
  interface Props {
    /** What runs, from the catalogue; rendered as text. */
    label: string;
  }

  let { label }: Props = $props();
</script>

<span class="waiting-mark" role="status" data-testid="waiting-mark">
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    width="20"
    height="20"
    class="waiting-mark__mark"
    aria-hidden="true"
    fill="none"
    stroke="currentColor"
    stroke-width="1.5"
    stroke-linecap="round"
    stroke-linejoin="round"
    data-testid="waiting-mark-glyph"
  >
    <path
      d="M8.5 4.5H6.75a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10.5a2 2 0 0 0 2-2v-13a2 2 0 0 0-2-2H15.5"
    />
    <rect x="8.5" y="2.75" width="7" height="3.5" rx="1" />
    <path d="M13.25 10.5h3M13.25 14.25h3M13.25 18h3" />
    <circle class="waiting-mark__dot waiting-mark__dot--1" cx="9.2" cy="10.5" r="1" />
    <circle class="waiting-mark__dot waiting-mark__dot--2" cx="9.2" cy="14.25" r="1" />
    <circle class="waiting-mark__dot waiting-mark__dot--3" cx="9.2" cy="18" r="1" />
    <path class="waiting-mark__tick waiting-mark__tick--1" d="M7.4 10.6l1.3 1.3 2.3-2.8" pathLength="1" />
    <path class="waiting-mark__tick waiting-mark__tick--2" d="M7.4 14.35l1.3 1.3 2.3-2.8" pathLength="1" />
    <path class="waiting-mark__tick waiting-mark__tick--3" d="M7.4 18.1l1.3 1.3 2.3-2.8" pathLength="1" />
  </svg>
  <span class="waiting-mark__label">{label}</span>
</span>

<style>
  .waiting-mark {
    display: inline-flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--line-height-sm);
  }

  .waiting-mark__mark {
    flex: 0 0 auto;
  }

  .waiting-mark__dot {
    fill: currentColor;
    stroke: none;
  }

  /* An untouched tick is undrawn: its one dash is shifted off the path. */
  .waiting-mark__tick {
    stroke-dasharray: 1;
    stroke-dashoffset: 1;
    opacity: 0;
  }

  /*
   * One cycle is 3.6 s: each tick is drawn in 0.3 s, 0.6 s after the one before (0 s, 0.6 s, 1.2 s),
   * the full list holds until 2.7 s, the ticks fade until 3.3 s, and the open list rests until the
   * cycle starts again. An open line's dot leaves when its tick starts and returns after the fade.
   * Every cycle opens on the reduced-motion rest state (dots shown, ticks undrawn), so the first
   * frame at 20 px never shows a line without its dot or its tick.
   */
  .waiting-mark__tick--1 {
    animation: waiting-mark-tick-1 3.6s linear infinite;
  }

  .waiting-mark__tick--2 {
    animation: waiting-mark-tick-2 3.6s linear infinite;
  }

  .waiting-mark__tick--3 {
    animation: waiting-mark-tick-3 3.6s linear infinite;
  }

  .waiting-mark__dot--1 {
    animation: waiting-mark-dot-1 3.6s linear infinite;
  }

  .waiting-mark__dot--2 {
    animation: waiting-mark-dot-2 3.6s linear infinite;
  }

  .waiting-mark__dot--3 {
    animation: waiting-mark-dot-3 3.6s linear infinite;
  }

  @keyframes waiting-mark-tick-1 {
    0% {
      stroke-dashoffset: 1;
      opacity: 0;
    }
    0.001% {
      stroke-dashoffset: 1;
      opacity: 1;
    }
    8.333%,
    75% {
      stroke-dashoffset: 0;
      opacity: 1;
    }
    91.667%,
    100% {
      stroke-dashoffset: 0;
      opacity: 0;
    }
  }

  @keyframes waiting-mark-tick-2 {
    0%,
    16.667% {
      stroke-dashoffset: 1;
      opacity: 0;
    }
    16.668% {
      stroke-dashoffset: 1;
      opacity: 1;
    }
    25%,
    75% {
      stroke-dashoffset: 0;
      opacity: 1;
    }
    91.667%,
    100% {
      stroke-dashoffset: 0;
      opacity: 0;
    }
  }

  @keyframes waiting-mark-tick-3 {
    0%,
    33.333% {
      stroke-dashoffset: 1;
      opacity: 0;
    }
    33.334% {
      stroke-dashoffset: 1;
      opacity: 1;
    }
    41.667%,
    75% {
      stroke-dashoffset: 0;
      opacity: 1;
    }
    91.667%,
    100% {
      stroke-dashoffset: 0;
      opacity: 0;
    }
  }

  @keyframes waiting-mark-dot-1 {
    0% {
      opacity: 1;
    }
    0.001%,
    91.667% {
      opacity: 0;
    }
    91.668%,
    100% {
      opacity: 1;
    }
  }

  @keyframes waiting-mark-dot-2 {
    0%,
    16.667% {
      opacity: 1;
    }
    16.668%,
    91.667% {
      opacity: 0;
    }
    91.668%,
    100% {
      opacity: 1;
    }
  }

  @keyframes waiting-mark-dot-3 {
    0%,
    33.333% {
      opacity: 1;
    }
    33.334%,
    91.667% {
      opacity: 0;
    }
    91.668%,
    100% {
      opacity: 1;
    }
  }

  /* Reduced motion: the list stands still and unticked, never stopped mid-cycle. */
  @media (prefers-reduced-motion: reduce) {
    .waiting-mark__tick,
    .waiting-mark__dot {
      animation: none;
    }

    .waiting-mark__tick {
      stroke-dashoffset: 1;
      opacity: 0;
    }

    .waiting-mark__dot {
      opacity: 1;
    }
  }
</style>
