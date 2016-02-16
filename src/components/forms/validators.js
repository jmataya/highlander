
export function ascii(value, label) {
  return /^[\x00-\x7F]+$/.test(value) ? null : `${label} must contain only ASCII characters`;
}

export function phoneNumber(value, label) {
  return /^[\d#\-\(\)\+\*) ]+$/.test(value) ? null : `${label} must not contain letters or other non-valid characters`;
}

export function email(value) {
  return /.+@.+/.test(value);
}
