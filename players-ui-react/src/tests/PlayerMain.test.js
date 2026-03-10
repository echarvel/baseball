import { render, screen } from '@testing-library/react';
import PlayerMain from '../components/PlayerMain';

test('renders main header', () => {
  render(<PlayerMain />);
  const element = screen.getByText("Hello Players");
  expect(element).toBeDefined();
});

test('renders search buttons', () => {
  render(<PlayerMain />);
  const element = screen.getByRole("button");
  expect(element).getByText("Player id");
  const element2 = screen.getByRole("button");
  expect(element2).getByText("Player Country Code");
});
