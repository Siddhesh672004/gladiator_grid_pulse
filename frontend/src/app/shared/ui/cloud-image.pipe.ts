import { Pipe, PipeTransform } from '@angular/core';

// cloudinary link -> a lighter version of the same picture: <img [src]="url | cloudImage:400">
// pass about twice the displayed width so it stays sharp on phones
@Pipe({
  name: 'cloudImage',
  standalone: true
})
export class CloudImagePipe implements PipeTransform {

  transform(url: string | null | undefined, width: number): string {
    if (!url) {
      return '';
    }
    return url.includes('/upload/') ? url.replace('/upload/', `/upload/f_auto,q_auto,c_limit,w_${width}/`) : url;
  }
}
