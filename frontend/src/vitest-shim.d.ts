// Ambient type shim for 'vitest'. The vitest package is not installed in this
// environment (and cannot be fetched offline), so this declaration lets the
// project typecheck. When vitest is installed (e.g. CI), the real module takes
// precedence over this declaration.
declare module 'vitest' {
  export function describe(name: string, fn: () => void): void;
  export function test(name: string, fn: () => void | Promise<void>): void;
  export function expect(actual: unknown): any;
  export const vi: any;
  export function beforeEach(fn: () => void): void;
  export function afterEach(fn: () => void): void;
}
