'use client';

import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { StrategyGoal } from '@/types/strategy-goal';

// Types for link data
export interface PdpGoalNode {
  id: string;
  personId: string;
  personName: string;
  title: string;
  status: 'ACTIVE' | 'ACHIEVED' | 'PAUSED' | 'DROPPED';
}

export interface LinkData {
  strategyGoalId: string;
  pdpGoal: PdpGoalNode;
}

interface SpiderWebVisualizationProps {
  goals: StrategyGoal[];
  links: LinkData[];
  onNodeClick?: (id: string, type: 'strategy' | 'pdp') => void;
  hideSensitiveContent?: boolean;
}

// Node positions for layout
interface NodePosition {
  x: number;
  y: number;
  radius: number;
}

export default function SpiderWebVisualization({
  goals,
  links,
  onNodeClick,
  hideSensitiveContent = false,
}: SpiderWebVisualizationProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 800, height: 600 });
  const [hoveredNodeId, setHoveredNodeId] = useState<string | null>(null);
  const [hoveredNodeType, setHoveredNodeType] = useState<'strategy' | 'pdp' | null>(null);

  // Handle responsive sizing
  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current) {
        const { offsetWidth } = containerRef.current;
        setDimensions({
          width: Math.max(400, offsetWidth),
          height: Math.max(400, Math.min(700, offsetWidth * 0.65)),
        });
      }
    };

    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  // Calculate node positions
  const { strategyPositions, pdpPositions, connections } = useMemo(() => {
    const { width, height } = dimensions;
    const centerX = width / 2;
    const strategyY = Math.max(80, height * 0.15);
    const pdpAreaTop = height * 0.42;
    const pdpAreaBottom = height * 0.9;
    
    // Filter to active goals for visualization
    const activeGoals = goals.filter(g => g.status === 'ACTIVE');
    const goalCount = activeGoals.length;
    
    // Strategy goals positioned in an arc at the top
    const strategyPositions: Record<string, NodePosition> = {};
    const baseRadius = Math.min(width, height) * 0.06;
    
    if (goalCount === 0) {
      return { strategyPositions: {}, pdpPositions: {}, connections: [] };
    }
    
    // Calculate arc positions for strategy goals
    const arcWidth = Math.min(width * 0.85, goalCount * 140);
    const startX = centerX - arcWidth / 2;
    const arcHeight = 40;
    
    activeGoals.forEach((goal, index) => {
      const progress = goalCount === 1 ? 0.5 : index / (goalCount - 1);
      const x = startX + arcWidth * progress;
      const y = strategyY + Math.sin(progress * Math.PI) * arcHeight;
      strategyPositions[goal.id] = {
        x,
        y,
        radius: goal.sensitive ? baseRadius * 1.1 : baseRadius,
      };
    });

    // Group PDP goals by their linked strategy goal
    const pdpByStrategy: Record<string, LinkData[]> = {};
    links.forEach(link => {
      if (!pdpByStrategy[link.strategyGoalId]) {
        pdpByStrategy[link.strategyGoalId] = [];
      }
      pdpByStrategy[link.strategyGoalId].push(link);
    });

    // Position PDP goals in a fan pattern around each strategy goal
    const pdpPositions: Record<string, NodePosition> = {};
    const connections: Array<{
      strategyId: string;
      pdpId: string;
      x1: number;
      y1: number;
      x2: number;
      y2: number;
    }> = [];
    const pdpRadius = baseRadius * 0.65;
    const minDistance = Math.max(120, Math.min(180, height * 0.25));
    const maxSpreadAngle = Math.PI / 2.5;
    
    Object.entries(pdpByStrategy).forEach(([strategyId, pdpLinks]) => {
      const strategyPos = strategyPositions[strategyId];
      if (!strategyPos) return;
      
      const goalCount = pdpLinks.length;
      
      pdpLinks.forEach((link, index) => {
        // Skip if this PDP goal was already positioned (duplicate ID)
        if (pdpPositions[link.pdpGoal.id]) {
          console.warn(`Duplicate PDP goal ID detected: ${link.pdpGoal.id}, skipping positioning`);
          return;
        }
        
        const angleStep = goalCount > 1 ? maxSpreadAngle / (goalCount - 1) : 0;
        const angle = -maxSpreadAngle / 2 + index * angleStep;
        
        const distance = minDistance + (index % 2) * 20;
        
        const x = strategyPos.x + Math.sin(angle) * distance;
        const y = strategyPos.y + Math.cos(angle) * distance;
        
        const clampedX = Math.max(pdpRadius + 20, Math.min(width - pdpRadius - 20, x));
        const clampedY = Math.max(strategyPos.y + 80, Math.min(height - 60, y));
        
        pdpPositions[link.pdpGoal.id] = {
          x: clampedX,
          y: clampedY,
          radius: pdpRadius,
        };
        
        connections.push({
          strategyId: strategyId,
          pdpId: link.pdpGoal.id,
          x1: strategyPos.x,
          y1: strategyPos.y + strategyPos.radius,
          x2: clampedX,
          y2: clampedY - pdpRadius,
        });
      });
    });

    return { strategyPositions, pdpPositions, connections };
  }, [goals, links, dimensions]);

  // Check if a node is highlighted (connected to hovered node)
  const isHighlighted = useCallback((id: string, type: 'strategy' | 'pdp') => {
    if (!hoveredNodeId || !hoveredNodeType) return true; // All visible when nothing hovered
    
    if (hoveredNodeType === 'strategy' && type === 'pdp') {
      // Check if this PDP goal is linked to the hovered strategy goal
      return links.some(l => l.strategyGoalId === hoveredNodeId && l.pdpGoal.id === id);
    }
    if (hoveredNodeType === 'pdp' && type === 'strategy') {
      // Check if this strategy goal is linked to the hovered PDP goal
      return links.some(l => l.pdpGoal.id === hoveredNodeId && l.strategyGoalId === id);
    }
    return hoveredNodeId === id;
  }, [hoveredNodeId, hoveredNodeType, links]);

  const isConnectionHighlighted = useCallback((strategyId: string, pdpId: string) => {
    if (!hoveredNodeId) return true;
    return (hoveredNodeId === strategyId || hoveredNodeId === pdpId);
  }, [hoveredNodeId]);

  // Handle node click
  const handleNodeClick = useCallback((id: string, type: 'strategy' | 'pdp') => {
    onNodeClick?.(id, type);
  }, [onNodeClick]);

  // Handle hover
  const handleMouseEnter = useCallback((id: string, type: 'strategy' | 'pdp') => {
    setHoveredNodeId(id);
    setHoveredNodeType(type);
  }, []);

  const handleMouseLeave = useCallback(() => {
    setHoveredNodeId(null);
    setHoveredNodeType(null);
  }, []);

  // Get status color
  const getStatusColor = (status: string, isPdp: boolean = false) => {
    if (isPdp) {
      switch (status) {
        case 'ACHIEVED': return 'var(--color-success)';
        case 'PAUSED': return 'var(--color-warning)';
        case 'DROPPED': return 'var(--color-text-muted)';
        default: return 'var(--color-secondary)';
      }
    }
    switch (status) {
      case 'ACHIEVED': return 'var(--color-success)';
      case 'DROPPED': return 'var(--color-text-muted)';
      default: return 'var(--color-primary)';
    }
  };

  // No data state
  if (goals.length === 0) {
    return (
      <div
        ref={containerRef}
        style={{
          width: '100%',
          minHeight: '300px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
        }}
      >
        <p style={{ color: 'var(--color-text-muted)', fontFamily: 'var(--font-mono)' }}>
          No strategy goals to visualize
        </p>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      data-testid="spider-web-visualization"
      style={{ width: '100%', position: 'relative' }}
      role="img"
      aria-label="Spider web visualization showing strategy goals and linked PDP goals"
    >
      <svg
        width={dimensions.width}
        height={dimensions.height}
        viewBox={`0 0 ${dimensions.width} ${dimensions.height}`}
        style={{
          backgroundColor: 'var(--color-bg-surface)',
          borderRadius: 'var(--radius-medium)',
          border: '1px solid var(--color-border)',
        }}
      >
        {/* Definitions for effects */}
        <defs>
          {/* Glow filter for connections */}
          <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
          
          {/* Strong glow for highlighted elements */}
          <filter id="glow-strong" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur stdDeviation="5" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>

          {/* Node glow effect */}
          <filter id="node-glow" x="-100%" y="-100%" width="300%" height="300%">
            <feGaussianBlur stdDeviation="4" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        {/* Background grid pattern (subtle cyberpunk texture) */}
        <pattern
          id="grid"
          width="40"
          height="40"
          patternUnits="userSpaceOnUse"
          patternTransform="rotate(45)"
        >
          <line
            x1="0"
            y1="0"
            x2="40"
            y2="0"
            stroke="var(--color-border-subtle)"
            strokeWidth="0.5"
            opacity="0.3"
          />
        </pattern>
        <rect width="100%" height="100%" fill="url(#grid)" />

        {/* Connection lines */}
        <g>
          {connections.map((conn, index) => {
            const highlighted = isConnectionHighlighted(conn.strategyId, conn.pdpId);
            return (
              <line
                key={`conn-${conn.strategyId}-${conn.pdpId}-${index}`}
                x1={conn.x1}
                y1={conn.y1}
                x2={conn.x2}
                y2={conn.y2}
                stroke={highlighted ? '#06b6d4' : '#4b5563'}
                strokeWidth={highlighted ? 3 : 2}
                strokeOpacity={highlighted ? 1 : 0.8}
                style={{
                  transition: 'stroke 0.2s, stroke-opacity 0.2s',
                }}
              />
            );
          })}
        </g>

        {/* Strategy Goal nodes */}
        <g>
          {goals.filter(g => g.status === 'ACTIVE').map(goal => {
            const pos = strategyPositions[goal.id];
            if (!pos) return null;
            
            const highlighted = isHighlighted(goal.id, 'strategy');
            const color = getStatusColor(goal.status);
            const isSensitive = goal.sensitive && hideSensitiveContent;
            
            return (
              <g
                key={`strategy-${goal.id}`}
                onClick={() => handleNodeClick(goal.id, 'strategy')}
                onMouseEnter={() => handleMouseEnter(goal.id, 'strategy')}
                onMouseLeave={handleMouseLeave}
                style={{ cursor: onNodeClick ? 'pointer' : 'default' }}
                data-testid={`node-strategy-${goal.id}`}
                role="button"
                aria-label={`Strategy goal: ${goal.title}`}
              >
                {/* Outer glow ring */}
                <circle
                  cx={pos.x}
                  cy={pos.y}
                  r={pos.radius + 4}
                  fill="none"
                  stroke={color}
                  strokeWidth="2"
                  strokeOpacity={highlighted ? 0.4 : 0.1}
                  filter={highlighted ? 'url(#node-glow)' : 'none'}
                  style={{ transition: 'stroke-opacity 0.2s' }}
                />
                
                {/* Main node */}
                <circle
                  cx={pos.x}
                  cy={pos.y}
                  r={pos.radius}
                  fill={highlighted ? color : 'var(--color-bg-elevated)'}
                  stroke={color}
                  strokeWidth="2"
                  style={{
                    transition: 'fill 0.2s, opacity 0.2s',
                    opacity: highlighted ? 1 : 0.5,
                  }}
                />
                
                {/* Sensitive indicator */}
                {goal.sensitive && (
                  <text
                    x={pos.x + pos.radius + 8}
                    y={pos.y}
                    fill="var(--color-warning)"
                    fontSize="12"
                    fontFamily="var(--font-mono)"
                    textAnchor="start"
                    dominantBaseline="middle"
                    opacity={highlighted ? 1 : 0.5}
                  >
                    🔒
                  </text>
                )}
                
                {/* Label */}
                <text
                  x={pos.x}
                  y={pos.y + pos.radius + 20}
                  fill={highlighted ? 'var(--color-text-primary)' : 'var(--color-text-secondary)'}
                  fontSize="11"
                  fontFamily="var(--font-mono)"
                  fontWeight="500"
                  textAnchor="middle"
                  style={{
                    transition: 'fill 0.2s, opacity 0.2s',
                    opacity: highlighted ? 1 : 0.5,
                  }}
                >
                  {isSensitive ? '•••' : goal.title.length > 20 ? `${goal.title.slice(0, 18)}…` : goal.title}
                </text>
              </g>
            );
          })}
        </g>

        {/* PDP Goal nodes */}
        <g>
          {links.map(link => {
            const pos = pdpPositions[link.pdpGoal.id];
            if (!pos) return null;
            
            const highlighted = isHighlighted(link.pdpGoal.id, 'pdp');
            const color = getStatusColor(link.pdpGoal.status, true);
            
            return (
              <g
                key={`pdp-${link.pdpGoal.id}`}
                onClick={() => handleNodeClick(link.pdpGoal.id, 'pdp')}
                onMouseEnter={() => handleMouseEnter(link.pdpGoal.id, 'pdp')}
                onMouseLeave={handleMouseLeave}
                style={{ cursor: onNodeClick ? 'pointer' : 'default' }}
                data-testid={`node-pdp-${link.pdpGoal.id}`}
                role="button"
                aria-label={`PDP goal: ${link.pdpGoal.title} (person: ${link.pdpGoal.personName})`}
              >
                {/* Main node */}
                <circle
                  cx={pos.x}
                  cy={pos.y}
                  r={pos.radius}
                  fill={highlighted ? color : 'var(--color-bg-elevated)'}
                  stroke={color}
                  strokeWidth="1.5"
                  style={{
                    transition: 'fill 0.2s, opacity 0.2s',
                    opacity: highlighted ? 1 : 0.4,
                  }}
                />
                
                {/* Label */}
                <text
                  x={pos.x}
                  y={pos.y + pos.radius + 14}
                  fill={highlighted ? 'var(--color-text-primary)' : 'var(--color-text-muted)'}
                  fontSize="9"
                  fontFamily="var(--font-mono)"
                  textAnchor="middle"
                  style={{
                    transition: 'fill 0.2s, opacity 0.2s',
                    opacity: highlighted ? 1 : 0.4,
                  }}
                >
                  {link.pdpGoal.title.length > 15 ? `${link.pdpGoal.title.slice(0, 13)}…` : link.pdpGoal.title}
                </text>
                
                {/* Person name */}
                <text
                  x={pos.x}
                  y={pos.y + pos.radius + 26}
                  fill={highlighted ? 'var(--color-text-secondary)' : 'var(--color-text-muted)'}
                  fontSize="8"
                  fontFamily="var(--font-ui)"
                  fontStyle="italic"
                  textAnchor="middle"
                  style={{
                    transition: 'fill 0.2s, opacity 0.2s',
                    opacity: highlighted ? 0.8 : 0.3,
                  }}
                >
                  {link.pdpGoal.personName}
                </text>
              </g>
            );
          })}
        </g>
      </svg>

      {/* Legend */}
      <div
        style={{
          display: 'flex',
          gap: 'var(--space-4)',
          marginTop: 'var(--space-4)',
          padding: 'var(--space-3) var(--space-4)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-medium)',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <div
            style={{
              width: 12,
              height: 12,
              borderRadius: '50%',
              backgroundColor: 'var(--color-primary)',
            }}
          />
          <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', fontFamily: 'var(--font-mono)' }}>
            Strategy Goal
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <div
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              backgroundColor: 'var(--color-secondary)',
            }}
          />
          <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', fontFamily: 'var(--font-mono)' }}>
            PDP Goal
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <div
            style={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              backgroundColor: 'var(--color-success)',
            }}
          />
          <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', fontFamily: 'var(--font-mono)' }}>
            Achieved
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
          <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-warning)' }}>🔒</span>
          <span style={{ fontSize: 'var(--text-caption)', color: 'var(--color-text-secondary)', fontFamily: 'var(--font-mono)' }}>
            Sensitive
          </span>
        </div>
      </div>
    </div>
  );
}
