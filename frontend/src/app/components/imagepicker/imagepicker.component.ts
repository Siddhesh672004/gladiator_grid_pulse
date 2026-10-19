import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ImageService } from '../../services/image.service';
import { PICTURE_MAX_MB, pictureProblem } from '../../validation';
import { byIndex } from '../../shared/track-by';

// "add pictures" box: each chosen file goes to cloudinary at once (ImageService), the page saves the links with its form
@Component({
  selector: 'app-imagepicker',
  templateUrl: './imagepicker.component.html'
})
export class ImagepickerComponent {

  @Input() urls: string[] = [];
  @Input() max: number = 5;
  @Input() kind: string = 'track';         // 'track' or 'profile' = which upload url
  @Input() inputId: string = 'pictures';
  @Output() urlsChange = new EventEmitter<string[]>();

  maxMb: number = PICTURE_MAX_MB;
  uploading: number = 0;
  errorMessage: string = '';

  readonly byIndex = byIndex;

  constructor(private imageService: ImageService) { }

  get roomLeft(): number {
    return this.max - this.urls.length - this.uploading;
  }

  choose(event: Event): void {
    const fileBox = event.target as HTMLInputElement;
    const files = Array.from(fileBox.files ?? []);
    fileBox.value = '';
    this.errorMessage = '';
    if (files.length > this.roomLeft) {
      this.errorMessage = this.tooManyText();
      return;
    }
    // one bad file = nothing is uploaded, the message names the first broken rule (pictureProblem)
    for (const file of files) {
      const problem = pictureProblem(file);
      if (problem) {
        this.errorMessage = problem;
        return;
      }
    }
    for (const file of files) {
      this.upload(file);
    }
  }

  remove(url: string): void {
    this.errorMessage = '';
    this.setUrls(this.urls.filter(link => link !== url));
  }

  // the track text is the one the backend answers (TRACK_PICTURES_TOO_MANY in java)
  private tooManyText(): string {
    if (this.kind === 'track') {
      return `A track can have at most ${this.max} pictures`;
    }
    return 'Only 1 profile picture can be added';
  }

  // the backend names what went wrong (not a picture, cloudinary down), no answer at all = the network
  private upload(file: File): void {
    this.uploading++;
    this.imageService.upload(this.kind, file).subscribe({
      next: (image) => {
        this.uploading--;
        this.setUrls(this.urls.concat(image.url));
      },
      error: (err) => {
        this.uploading--;
        this.errorMessage = err.error?.message || 'The picture could not be uploaded. Please try again.';
      }
    });
  }

  private setUrls(urls: string[]): void {
    this.urls = urls;
    this.urlsChange.emit(urls);
  }
}
