import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  cleanup();
  localStorage.clear();
});

// Quiet known React prop-forwarding warnings that come from MUI internals
// (these are noisy in tests but not actionable for our app). We only filter
// a few specific messages to avoid hiding real problems.
const _origConsoleError = console.error;
console.error = (...args) => {
  try {
    const msg = String(args[0]);

    const IGNORED_PATTERNS = [
      "React does not recognize the `textAlign` prop",
      "React does not recognize the `InputProps` prop",
      "React does not recognize the `inputProps` prop",
      "React does not recognize the `inputadornmentprops` prop",
      "Received `true` for a non-boolean attribute `item`",
    ];

    if (IGNORED_PATTERNS.some((p) => msg.includes(p))) return;
  } catch (e) {
    // fall through to original
  }

  _origConsoleError.apply(console, args);
};
