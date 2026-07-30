// File: packages/ai-agents/src/orchestrator/OrchestratorAgent.ts

import { LangGraphRunnableConfig } from "@langchain/langgraph";
import { MessagesAnnotation, StateGraph, END } from "@langchain/langgraph";
import { HumanMessage, AIMessage } from "@langchain/core/messages";
import { IntentDetector } from "./IntentDetector";
import { TaskPlanner } from "./TaskPlanner";
import { AgentRouter } from "./AgentRouter";
import { ResponseAggregator } from "./ResponseAggregator";
import { ToolRegistry } from "../tools/ToolRegistry";

export interface Message {
  role: 'user' | 'assistant';
  content: string;
}

export interface OrchestrationResult {
  success: boolean;
  message: string;
  actions: AgentAction[];
  data: any;
  error?: string;
}

export interface AgentAction {
  agent: string;
  action: string;
  params: any;
  result: any;
}

export class OrchestratorAgent {
  private intentDetector: IntentDetector;
  private taskPlanner: TaskPlanner;
  private agentRouter: AgentRouter;
  private responseAggregator: ResponseAggregator;
  private toolRegistry: ToolRegistry;

  constructor(
    private llmProvider: any, // LLM provider instance (OpenAI, Azure, etc.)
    private sessionMemory: SessionMemory
  ) {
    this.intentDetector = new IntentDetector(llmProvider);
    this.taskPlanner = new TaskPlanner(llmProvider);
    this.agentRouter = new AgentRouter();
    this.responseAggregator = new ResponseAggregator();
    this.toolRegistry = new ToolRegistry();
  }

  /**
   * Main orchestration flow
   */
  async process(userMessage: string, sessionId: string): Promise<OrchestrationResult> {
    try {
      // Step 1: Detect Intent
      const intents = await this.detectIntents(userMessage);
      console.log(`🎯 Detected Intents:`, intents);

      // Step 2: Plan Tasks
      const tasks = await this.planTasks(userMessage, intents);
      console.log(`📋 Planned Tasks:`, tasks);

      // Step 3: Route to Agents
      const agentActions = await this.routeAndExecute(tasks, sessionId);
      console.log(`✅ Agent Actions:`, agentActions);

      // Step 4: Aggregate Responses
      const finalResponse = await this.responseAggregator.aggregate(
        userMessage,
        intents,
        agentActions
      );

      return {
        success: true,
        message: finalResponse.message,
        actions: agentActions,
        data: finalResponse.data
      };
    } catch (error) {
      console.error(`❌ Orchestration Error:`, error);
      return {
        success: false,
        message: 'Failed to process your request',
        actions: [],
        data: null,
        error: error.message
      };
    }
  }

  /**
   * Step 1: Intent Detection via LLM
   */
  private async detectIntents(userMessage: string) {
    return await this.intentDetector.detect(userMessage);
  }

  /**
   * Step 2: Task Planning
   */
  private async planTasks(userMessage: string, intents: Intent[]) {
    return await this.taskPlanner.plan(userMessage, intents);
  }

  /**
   * Step 3: Route to Agents and Execute in Parallel
   */
  private async routeAndExecute(tasks: Task[], sessionId: string): Promise<AgentAction[]> {
    const agentActions: AgentAction[] = [];

    // Execute all tasks in parallel
    const promises = tasks.map(async (task) => {
      try {
        // Get appropriate agent
        const agent = this.agentRouter.route(task.type);

        // Execute agent
        console.log(`🤖 Executing ${agent.name}...`);
        const result = await agent.execute(task.params);

        agentActions.push({
          agent: agent.name,
          action: task.type,
          params: task.params,
          result
        });

        // Store in session memory
        await this.sessionMemory.addMessage({
          role: 'assistant',
          content: `Agent ${agent.name} completed action ${task.type}`
        });
      } catch (error) {
        agentActions.push({
          agent: 'Unknown',
          action: task.type,
          params: task.params,
          result: { error: error.message }
        });
      }
    });

    await Promise.all(promises);
    return agentActions;
  }
}

// ============================================
// File: packages/ai-agents/src/orchestrator/IntentDetector.ts

export interface Intent {
  type: string; // SEARCH_MENU, ADD_TO_CART, BOOK_TABLE, etc.
  confidence: number;
  entities: {
    category?: string;
    dietaryType?: string;
    itemName?: string;
    quantity?: number;
    partySize?: number;
    date?: string;
    time?: string;
    [key: string]: any;
  };
}

export class IntentDetector {
  private systemPrompt = `You are an intent detection system for a restaurant ordering platform.

Analyze user messages and extract:
1. Intent type (one of: SEARCH_MENU, ADD_TO_CART, REMOVE_FROM_CART, VIEW_CART, BOOK_TABLE,
   CHECKOUT, APPLY_COUPON, GET_RECOMMENDATION)
2. Confidence score (0-1)
3. Entities (category, dietaryType, itemName, quantity, partySize, date, time, etc.)

Return response as JSON array of intents.`;

  constructor(private llmProvider: any) {}

  async detect(userMessage: string): Promise<Intent[]> {
    const response = await this.llmProvider.chat.completions.create({
      model: 'gpt-4',
      messages: [
        { role: 'system', content: this.systemPrompt },
        { role: 'user', content: userMessage }
      ],
      response_format: { type: 'json_object' }
    });

    const content = response.choices[0].message.content;
    const parsed = JSON.parse(content);

    // Normalize response
    return Array.isArray(parsed.intents) ? parsed.intents : [parsed];
  }
}

// ============================================
// File: packages/ai-agents/src/orchestrator/TaskPlanner.ts

export interface Task {
  id: string;
  type: string;
  params: any;
  priority: number;
  dependsOn?: string[];
}

export class TaskPlanner {
  private systemPrompt = `You are a task planner for a restaurant ordering system.

Given intents, create a detailed plan of tasks to execute in order.
Consider:
- Task dependencies (e.g., calculate price after adding items)
- Parallel execution opportunities
- User context and preferences

Return as JSON with tasks array, each containing:
{ id, type, params, priority, dependsOn? }`;

  constructor(private llmProvider: any) {}

  async plan(userMessage: string, intents: Intent[]): Promise<Task[]> {
    const response = await this.llmProvider.chat.completions.create({
      model: 'gpt-4',
      messages: [
        { role: 'system', content: this.systemPrompt },
        {
          role: 'user',
          content: `User message: "${userMessage}"\n\nDetected intents: ${JSON.stringify(intents)}`
        }
      ],
      response_format: { type: 'json_object' }
    });

    const content = response.choices[0].message.content;
    const parsed = JSON.parse(content);

    return parsed.tasks || [];
  }
}

// ============================================
// File: packages/ai-agents/src/orchestrator/AgentRouter.ts

import { MenuAgent } from '../agents/MenuAgent';
import { CartAgent } from '../agents/CartAgent';
import { BookingAgent } from '../agents/BookingAgent';
import { PricingAgent } from '../agents/PricingAgent';
import { CheckoutAgent } from '../agents/CheckoutAgent';

export interface Agent {
  name: string;
  supportedActions: string[];
  execute(params: any): Promise<any>;
}

export class AgentRouter {
  private agents: Map<string, Agent> = new Map();

  constructor() {
    this.registerAgents();
  }

  private registerAgents() {
    this.agents.set('SEARCH_MENU', new MenuAgent());
    this.agents.set('GET_RECOMMENDATION', new MenuAgent());
    this.agents.set('ADD_TO_CART', new CartAgent());
    this.agents.set('REMOVE_FROM_CART', new CartAgent());
    this.agents.set('VIEW_CART', new CartAgent());
    this.agents.set('UPDATE_CART', new CartAgent());
    this.agents.set('BOOK_TABLE', new BookingAgent());
    this.agents.set('CHECK_AVAILABILITY', new BookingAgent());
    this.agents.set('CALCULATE_PRICE', new PricingAgent());
    this.agents.set('APPLY_COUPON', new PricingAgent());
    this.agents.set('CHECKOUT', new CheckoutAgent());
  }

  route(intentType: string): Agent {
    const agent = this.agents.get(intentType);
    if (!agent) {
      throw new Error(`No agent found for intent type: ${intentType}`);
    }
    return agent;
  }
}

// ============================================
// File: packages/ai-agents/src/orchestrator/ResponseAggregator.ts

export interface AggregatedResponse {
  message: string;
  data: any;
}

export class ResponseAggregator {
  async aggregate(
    userMessage: string,
    intents: Intent[],
    agentActions: AgentAction[]
  ): Promise<AggregatedResponse> {
    // Build a natural language response combining all agent results
    const actionSummaries = agentActions
      .filter(action => !action.result.error)
      .map(action => this.summarizeAction(action))
      .join('\n');

    const errors = agentActions
      .filter(action => action.result.error)
      .map(action => `❌ ${action.agent}: ${action.result.error}`)
      .join('\n');

    const message = [
      actionSummaries,
      errors
    ]
      .filter(Boolean)
      .join('\n\n');

    return {
      message: message || 'Your request could not be processed',
      data: {
        actions: agentActions,
        intents
      }
    };
  }

  private summarizeAction(action: AgentAction): string {
    const result = action.result;

    switch (action.agent) {
      case 'MenuAgent':
        return `🍽️ Found ${result.items?.length || 0} menu items`;
      case 'CartAgent':
        return `🛒 ${result.message}`;
      case 'BookingAgent':
        return `✅ ${result.message}`;
      case 'PricingAgent':
        return `💰 Total: ₹${result.total}`;
      case 'CheckoutAgent':
        return `📦 Order ${result.orderId} created`;
      default:
        return `✅ ${result.message}`;
    }
  }
}

// ============================================
// File: packages/ai-agents/src/agents/MenuAgent.ts

import { SearchMenuUseCase, SearchMenuRequest } from '@restaurant/application/use-cases';

export class MenuAgent implements Agent {
  name = 'MenuAgent';
  supportedActions = ['SEARCH_MENU', 'GET_RECOMMENDATION'];

  constructor(
    private searchMenuUseCase: SearchMenuUseCase
  ) {}

  async execute(params: any): Promise<any> {
    const request: SearchMenuRequest = {
      query: params.search,
      category: params.category,
      dietaryType: params.dietaryType,
      priceRange: params.priceRange,
      minRating: params.minRating
    };

    const response = await this.searchMenuUseCase.execute(request);
    return response;
  }
}

// ============================================
// File: packages/ai-agents/src/agents/CartAgent.ts

import {
  AddToCartUseCase,
  ViewCartUseCase,
  UpdateCartUseCase,
  RemoveFromCartUseCase
} from '@restaurant/application/use-cases';

export class CartAgent implements Agent {
  name = 'CartAgent';
  supportedActions = ['ADD_TO_CART', 'REMOVE_FROM_CART', 'VIEW_CART', 'UPDATE_CART'];

  constructor(
    private addToCartUseCase: AddToCartUseCase,
    private removeFromCartUseCase: RemoveFromCartUseCase,
    private viewCartUseCase: ViewCartUseCase,
    private updateCartUseCase: UpdateCartUseCase
  ) {}

  async execute(params: any): Promise<any> {
    switch (params.action) {
      case 'ADD_TO_CART':
        return await this.addToCartUseCase.execute({
          cartId: params.cartId,
          menuItemId: params.menuItemId,
          quantity: params.quantity,
          specialInstructions: params.specialInstructions
        });

      case 'REMOVE_FROM_CART':
        return await this.removeFromCartUseCase.execute({
          cartId: params.cartId,
          menuItemId: params.menuItemId
        });

      case 'VIEW_CART':
        return await this.viewCartUseCase.execute({
          cartId: params.cartId
        });

      case 'UPDATE_CART':
        return await this.updateCartUseCase.execute({
          cartId: params.cartId,
          menuItemId: params.menuItemId,
          quantity: params.quantity
        });

      default:
        throw new Error(`Unsupported action: ${params.action}`);
    }
  }
}

// ============================================
// File: packages/ai-agents/src/agents/BookingAgent.ts

import {
  BookTableUseCase,
  CheckAvailabilityUseCase
} from '@restaurant/application/use-cases';

export class BookingAgent implements Agent {
  name = 'BookingAgent';
  supportedActions = ['BOOK_TABLE', 'CHECK_AVAILABILITY'];

  constructor(
    private bookTableUseCase: BookTableUseCase,
    private checkAvailabilityUseCase: CheckAvailabilityUseCase
  ) {}

  async execute(params: any): Promise<any> {
    switch (params.action) {
      case 'BOOK_TABLE':
        return await this.bookTableUseCase.execute({
          customerId: params.customerId,
          partySize: params.partySize,
          date: params.date,
          time: params.time,
          specialRequests: params.specialRequests
        });

      case 'CHECK_AVAILABILITY':
        return await this.checkAvailabilityUseCase.execute({
          partySize: params.partySize,
          date: params.date,
          time: params.time
        });

      default:
        throw new Error(`Unsupported action: ${params.action}`);
    }
  }
}

// ============================================
// File: packages/ai-agents/src/agents/PricingAgent.ts

import {
  CalculatePriceUseCase,
  ApplyCouponUseCase
} from '@restaurant/application/use-cases';

export class PricingAgent implements Agent {
  name = 'PricingAgent';
  supportedActions = ['CALCULATE_PRICE', 'APPLY_COUPON'];

  constructor(
    private calculatePriceUseCase: CalculatePriceUseCase,
    private applyCouponUseCase: ApplyCouponUseCase
  ) {}

  async execute(params: any): Promise<any> {
    switch (params.action) {
      case 'CALCULATE_PRICE':
        return await this.calculatePriceUseCase.execute({
          cartId: params.cartId,
          couponCode: params.couponCode
        });

      case 'APPLY_COUPON':
        return await this.applyCouponUseCase.execute({
          cartId: params.cartId,
          couponCode: params.couponCode
        });

      default:
        throw new Error(`Unsupported action: ${params.action}`);
    }
  }
}

// ============================================
// File: packages/ai-agents/src/agents/CheckoutAgent.ts

import {
  CreateOrderUseCase,
  ProcessPaymentUseCase
} from '@restaurant/application/use-cases';

export class CheckoutAgent implements Agent {
  name = 'CheckoutAgent';
  supportedActions = ['CHECKOUT', 'CREATE_ORDER', 'PROCESS_PAYMENT'];

  constructor(
    private createOrderUseCase: CreateOrderUseCase,
    private processPaymentUseCase: ProcessPaymentUseCase
  ) {}

  async execute(params: any): Promise<any> {
    switch (params.action) {
      case 'CREATE_ORDER':
        return await this.createOrderUseCase.execute({
          customerId: params.customerId,
          cartId: params.cartId,
          deliveryType: params.deliveryType
        });

      case 'PROCESS_PAYMENT':
        return await this.processPaymentUseCase.execute({
          orderId: params.orderId,
          customerId: params.customerId,
          amount: params.amount,
          method: params.method,
          paymentDetails: params.paymentDetails
        });

      case 'CHECKOUT':
        // Full checkout flow: create order then process payment
        const orderResult = await this.createOrderUseCase.execute({
          customerId: params.customerId,
          cartId: params.cartId,
          deliveryType: params.deliveryType
        });

        const paymentResult = await this.processPaymentUseCase.execute({
          orderId: orderResult.orderId,
          customerId: params.customerId,
          amount: orderResult.total,
          method: params.method,
          paymentDetails: params.paymentDetails
        });

        return {
          orderId: orderResult.orderId,
          paymentId: paymentResult.paymentId,
          message: `Order created and payment processed successfully`
        };

      default:
        throw new Error(`Unsupported action: ${params.action}`);
    }
  }
}

