
export type FacetElementProps = {
  facet: string,
  value: string,
  label: string,
  checked?: boolean,
  click: (facet: string, value: string, checked: boolean) => void,
};

export type FacetValue = {
  label: string,
  value: Object|string,
  count: number,
}

export type Facet = {
  key: string,
  name: string,
  kind: 'color' | 'circle' | 'checkbox',
  values: Array<FacetValue>,
}
