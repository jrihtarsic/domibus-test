import {Directive, ElementRef, Renderer2, HostListener, Input, OnInit} from '@angular/core';
import {NG_VALUE_ACCESSOR, ControlValueAccessor, NgModel} from '@angular/forms';

@Directive({
  selector: '[input-debounce]',
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: InputDebounceBehaviourDirective,
    multi: true,
  }],
})
export class InputDebounceBehaviourDirective implements ControlValueAccessor, OnInit {

  onChange: Function;
  onTouched: Function;

  @Input('jrvDebounceControl')
  debounceTime: number;

  private _timer = null;

  private setValueFn: Function;
  private lastValue: string;

  constructor(private _elementRef: ElementRef, private _renderer: Renderer2) {
  }

  ngOnInit() {
    if (typeof this.debounceTime !== 'number') {
      this.debounceTime = 2000; // default debounce to 2sec
    }
  }

  @HostListener('input', ['$event.target.value'])
  input(value: string) {
    console.log('input:', value);
    this.lastValue = value;
    if (value) {
      this.onChange(value);
    } else {
      this.setValueFn(value);
    }
  }

  @HostListener('blur')
  onBlur() {
    if (this.setValueFn) {
      console.log('onBlur:', this.lastValue);
      if(this.lastValue !== undefined) {
        this.setValueFn(this.lastValue);
      }
    }
  }

  writeValue(value: string) {
    const element = this._elementRef.nativeElement;
    // console.log('writeValue:', element, value);
    this._renderer.setProperty(element, 'value', (value == null ? '' : value));
  }

  registerOnChange(fn: Function) {
    this.onChange = this._debounce(fn);
    this.setValueFn = fn;
  }

  registerOnTouched(fn: Function) {
    this.onTouched = this._debounce(fn);
  }

  // private
  private _debounce(fn: Function) {
    return (...args: any[]) => {
      if (this._timer) {
        clearTimeout(this._timer);
      }
      this._timer = setTimeout(() => {
        fn(...args);
        this._timer = null;
      }, this.debounceTime);
    }
  }
}
