import { CommonModule } from '@angular/common';
import { Component, ViewChild, ViewContainerRef } from '@angular/core';

// the round chat button on every page. the chat itself (ChatbotComponent with its forms and services) is
// downloaded on the first click only, opens right away and from then on draws its own button
@Component({
  selector: 'app-chatlauncher',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chat-launcher.component.html'
})
export class ChatLauncherComponent {

  loaded: boolean = false;
  private loading: boolean = false;

  @ViewChild('chatSlot', { read: ViewContainerRef, static: true }) chatSlot!: ViewContainerRef;

  open(): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    import('../chatbot/chatbot.component')
      .then(({ ChatbotComponent }) => {
        this.chatSlot.createComponent(ChatbotComponent).instance.toggle();
        this.loaded = true;
      })
      // not downloaded (no internet): the next click tries again
      .catch(() => this.loading = false);
  }
}
