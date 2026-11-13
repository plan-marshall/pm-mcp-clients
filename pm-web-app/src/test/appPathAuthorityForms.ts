/**
 * The address forms that name a HOST and no path of the app, shared by the tests of `Link` and of
 * `RowLink` so the two guards are held to one contract.
 *
 * An address that starts with two slashes, or a slash and a backslash, names a host, not a path of
 * the app (WHATWG URL spec, "special authority ignore slashes state"): a browser resolves it
 * against the page scheme and goes to that host. The parser strips every tab, carriage return and
 * line feed before it reads the address, so those characters inside or in front of the leading pair
 * change nothing. The placeholder origin of `isAppPath` is `app.invalid`; an address that names
 * exactly that host resolves to the placeholder origin and must still be refused, because on a real
 * deployment `app.invalid` is a foreign host. These literals are the expectation of a security
 * contract, not generated input.
 */
export const AUTHORITY_FORM: ReadonlyArray<readonly [string, string]> = [
  ['//app.invalid/settings', 'the placeholder host after two slashes'],
  ['//app.invalid', 'the placeholder host alone'],
  ['///app.invalid/x', 'the placeholder host after three slashes'],
  ['//evil.example/x', 'a foreign host after two slashes'],
  [String.raw`/\app.invalid/x`, 'the placeholder host after a slash and a backslash'],
  [String.raw`/\evil.example/x`, 'a foreign host after a slash and a backslash'],
  [String.raw`/\/app.invalid/x`, 'the placeholder host after a slash, a backslash and a slash'],
  ['  //app.invalid/settings  ', 'white space around two slashes'],
  ['\t//app.invalid/settings', 'a tab in front of two slashes'],
  ['\n//app.invalid/settings', 'a line feed in front of two slashes'],
  ['\r\n \t/\\app.invalid/x', 'a mix of white space in front of a slash and a backslash'],
  ['/\t/app.invalid/x', 'a tab between the two slashes'],
  ['/\n/app.invalid/x', 'a line feed between the two slashes'],
  ['/\r\\app.invalid/x', 'a carriage return between the slash and the backslash'],
  ['/\t\n\r/app.invalid/x', 'a tab, a line feed and a carriage return between the two slashes'],
  ['/\t/evil.example/x', 'a tab between the two slashes, a foreign host'],
  ['\t /\n/app.invalid/x', 'white space in front of two slashes split by a line feed'],
];
