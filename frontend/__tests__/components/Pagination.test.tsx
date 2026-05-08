import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Pagination from '@/components/Pagination';

describe('Pagination', () => {
  it('should render current page and total pages', () => {
    const onPageChange = jest.fn();
    render(<Pagination currentPage={0} totalPages={5} onPageChange={onPageChange} />);

    expect(screen.getByTestId('page-info')).toHaveTextContent('Page 1 of 5');
  });

  it('should render nothing when totalPages is 1', () => {
    const onPageChange = jest.fn();
    const { container } = render(<Pagination currentPage={0} totalPages={1} onPageChange={onPageChange} />);

    expect(container.innerHTML).toBe('');
  });

  it('should disable Previous button on first page', () => {
    const onPageChange = jest.fn();
    render(<Pagination currentPage={0} totalPages={5} onPageChange={onPageChange} />);

    const prevButton = screen.getByLabelText('Previous page');
    expect(prevButton).toBeDisabled();
  });

  it('should disable Next button on last page', () => {
    const onPageChange = jest.fn();
    render(<Pagination currentPage={4} totalPages={5} onPageChange={onPageChange} />);

    const nextButton = screen.getByLabelText('Next page');
    expect(nextButton).toBeDisabled();
  });

  it('should call onPageChange with next page when Next is clicked', () => {
    const onPageChange = jest.fn();
    render(<Pagination currentPage={2} totalPages={5} onPageChange={onPageChange} />);

    fireEvent.click(screen.getByLabelText('Next page'));
    expect(onPageChange).toHaveBeenCalledWith(3);
  });

  it('should call onPageChange with previous page when Previous is clicked', () => {
    const onPageChange = jest.fn();
    render(<Pagination currentPage={2} totalPages={5} onPageChange={onPageChange} />);

    fireEvent.click(screen.getByLabelText('Previous page'));
    expect(onPageChange).toHaveBeenCalledWith(1);
  });

  it('should display correct page info for middle page', () => {
    const onPageChange = jest.fn();
    render(<Pagination currentPage={2} totalPages={5} onPageChange={onPageChange} />);

    expect(screen.getByTestId('page-info')).toHaveTextContent('Page 3 of 5');
  });
});
