// one line of a drop-down box
export interface FieldOption {
  value: any;
  text: string;
}

// ['Circuit', 'Rally'] -> drop-down lines that show the value itself
export function toOptions(values: string[]): FieldOption[] {
  const options: FieldOption[] = [];
  for (const value of values) {
    options.push({ value: value, text: value });
  }
  return options;
}
