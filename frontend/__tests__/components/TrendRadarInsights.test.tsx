import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import TrendRadarInsights from '@/components/TrendRadarInsights';
import { generateTrendRadar } from '@/lib/api-client';

jest.mock('@/lib/api-client', () => ({
  generateTrendRadar: jest.fn(),
}));

const mockGenerateTrendRadar = generateTrendRadar as jest.MockedFunction<typeof generateTrendRadar>;

describe('TrendRadarInsights', () => {
  const defaultProps = {
    token: 'test-token',
    personId: 'person-123',
    personName: 'John Doe',
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render the component with empty state', () => {
    render(<TrendRadarInsights {...defaultProps} />);

    expect(screen.getByTestId('trend-radar-insights')).toBeInTheDocument();
    expect(screen.getByTestId('radar-empty-state')).toBeInTheDocument();
    expect(screen.getByTestId('generate-insights-button')).toHaveTextContent('Scan Radar');
    expect(screen.getByText(/Click "Scan Radar" to analyze 90 days of data for John Doe/)).toBeInTheDocument();
  });

  it('should show loading state when scanning', async () => {
    mockGenerateTrendRadar.mockImplementation(() => new Promise(() => {})); // never resolves

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    expect(screen.getByTestId('radar-loading')).toBeInTheDocument();
    expect(screen.getByText(/Analyzing 90-day data window/)).toBeInTheDocument();
  });

  it('should display insights on successful scan', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [
        { title: 'Burnout Risk', description: 'High workload detected.', dimension: 'MORALE', confidenceScore: 72 },
        { title: 'Growth Stagnation', description: 'No PDP updates.', dimension: 'WORK_GROWTH_BALANCE', confidenceScore: 55 },
        { title: 'Strong Output', description: 'Consistent completion.', dimension: 'RECOGNITION', confidenceScore: 85 },
      ],
      insufficientData: false,
      meetingsNeeded: null,
      error: null,
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-insights-list')).toBeInTheDocument();
    });

    expect(screen.getByTestId('insight-card-0')).toBeInTheDocument();
    expect(screen.getByTestId('insight-card-1')).toBeInTheDocument();
    expect(screen.getByTestId('insight-card-2')).toBeInTheDocument();
    expect(screen.getByText('Burnout Risk')).toBeInTheDocument();
    expect(screen.getByText('High workload detected.')).toBeInTheDocument();
    expect(screen.getByText('Growth Stagnation')).toBeInTheDocument();
    expect(screen.getByText('Strong Output')).toBeInTheDocument();
  });

  it('should display confidence scores with correct labels', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [
        { title: 'Low Signal', description: 'Thin data.', dimension: 'MORALE', confidenceScore: 25 },
        { title: 'Moderate Signal', description: 'Some data.', dimension: 'RECOGNITION', confidenceScore: 55 },
        { title: 'High Signal', description: 'Rich data.', dimension: 'MEETING_EFFICACY', confidenceScore: 85 },
      ],
      insufficientData: false,
      meetingsNeeded: null,
      error: null,
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-insights-list')).toBeInTheDocument();
    });

    expect(screen.getByTestId('insight-confidence-label-0')).toHaveTextContent('25% — Low Signal');
    expect(screen.getByTestId('insight-confidence-label-1')).toHaveTextContent('55% — Moderate Signal');
    expect(screen.getByTestId('insight-confidence-label-2')).toHaveTextContent('85% — High Signal');
  });

  it('should display dimension labels', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [
        { title: 'Test', description: 'Desc.', dimension: 'MORALE', confidenceScore: 50 },
        { title: 'Test2', description: 'Desc2.', dimension: 'WORK_GROWTH_BALANCE', confidenceScore: 60 },
        { title: 'Test3', description: 'Desc3.', dimension: 'MEETING_EFFICACY', confidenceScore: 70 },
      ],
      insufficientData: false,
      meetingsNeeded: null,
      error: null,
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-insights-list')).toBeInTheDocument();
    });

    expect(screen.getByTestId('insight-dimension-0')).toHaveTextContent('Morale');
    expect(screen.getByTestId('insight-dimension-1')).toHaveTextContent('Work/Growth');
    expect(screen.getByTestId('insight-dimension-2')).toHaveTextContent('Meeting Efficacy');
  });

  it('should show insufficient data state', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [],
      insufficientData: true,
      meetingsNeeded: 2,
      error: 'Scanning horizon... Need 2 more 1:1(s) to establish a baseline.',
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-insufficient-data')).toBeInTheDocument();
    });

    expect(screen.getByText(/Scanning horizon/)).toBeInTheDocument();
    expect(screen.getByText(/Need 2 more 1:1/)).toBeInTheDocument();
  });

  it('should show error state', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [],
      insufficientData: false,
      meetingsNeeded: null,
      error: 'AI API connection refused',
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-error')).toBeInTheDocument();
    });

    expect(screen.getByText('AI API connection refused')).toBeInTheDocument();
  });

  it('should handle API exception gracefully', async () => {
    mockGenerateTrendRadar.mockRejectedValue(new Error('Network error'));

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('should disable button while loading', async () => {
    mockGenerateTrendRadar.mockImplementation(() => new Promise(() => {}));

    render(<TrendRadarInsights {...defaultProps} />);

    const button = screen.getByTestId('generate-insights-button');
    fireEvent.click(button);

    expect(button).toBeDisabled();
    expect(button).toHaveTextContent('Scanning...');
  });

  it('should show Rescan button after successful scan', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [
        { title: 'Test', description: 'Desc.', dimension: 'MORALE', confidenceScore: 50 },
      ],
      insufficientData: false,
      meetingsNeeded: null,
      error: null,
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('radar-insights-list')).toBeInTheDocument();
    });

    expect(screen.getByTestId('generate-insights-button')).toHaveTextContent('Rescan');
  });

  it('should call generateTrendRadar with correct params', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [],
      insufficientData: true,
      meetingsNeeded: 3,
      error: 'Need more data',
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(mockGenerateTrendRadar).toHaveBeenCalledWith('test-token', 'person-123');
    });
  });

  it('should render confidence bar with correct width', async () => {
    mockGenerateTrendRadar.mockResolvedValue({
      insights: [
        { title: 'Test', description: 'Desc.', dimension: 'MORALE', confidenceScore: 72 },
      ],
      insufficientData: false,
      meetingsNeeded: null,
      error: null,
    });

    render(<TrendRadarInsights {...defaultProps} />);

    fireEvent.click(screen.getByTestId('generate-insights-button'));

    await waitFor(() => {
      expect(screen.getByTestId('insight-confidence-bar-0')).toBeInTheDocument();
    });

    expect(screen.getByTestId('insight-confidence-bar-0')).toHaveStyle({ width: '72%' });
  });
});
