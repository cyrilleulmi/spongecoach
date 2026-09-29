import { Component, computed, effect, input, output, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Question } from '../drill.model';

export interface QuestionAnswers {
  answers: { questionId: string; text: string }[];
  guess: boolean;
}

/**
 * The interpreter's Clarifying questions (ADR-0019): pick a suggested answer or write one, then
 * send; or let the interpreter decide ("Rate einfach"). Hovering a question highlights what it's
 * about on the sketch.
 */
@Component({
  selector: 'app-drill-questions',
  imports: [FormsModule],
  templateUrl: './drill-questions.html',
  styleUrl: './drill-questions.scss',
})
export class DrillQuestions {
  readonly questions = input.required<Question[]>();
  readonly busy = input(false);
  readonly answered = output<QuestionAnswers>();
  readonly focused = output<Question | null>();

  protected readonly texts = signal<Record<string, string>>({});
  protected readonly anyAnswer = computed(() => Object.values(this.texts()).some((text) => text.trim() !== ''));

  constructor() {
    effect(() => {
      this.questions();
      untracked(() => this.texts.set({}));
    });
  }

  protected setText(questionId: string, text: string): void {
    this.texts.update((texts) => ({ ...texts, [questionId]: text }));
  }

  protected send(): void {
    const answers = Object.entries(this.texts())
      .filter(([, text]) => text.trim() !== '')
      .map(([questionId, text]) => ({ questionId, text: text.trim() }));
    if (answers.length > 0) this.answered.emit({ answers, guess: false });
  }

  protected guess(): void {
    const answers = Object.entries(this.texts())
      .filter(([, text]) => text.trim() !== '')
      .map(([questionId, text]) => ({ questionId, text: text.trim() }));
    this.answered.emit({ answers, guess: true });
  }
}
