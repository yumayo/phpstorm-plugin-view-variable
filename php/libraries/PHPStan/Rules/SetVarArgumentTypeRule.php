<?php

declare(strict_types=1);

namespace App\Libraries\PHPStan\Rules;

use App\Modules\Cli\Controller\SampleController;
use PhpParser\Node;
use PhpParser\Node\Expr\MethodCall;
use PHPStan\Analyser\Scope;
use PHPStan\Rules\Rule;
use PHPStan\Rules\RuleErrorBuilder;
use PHPStan\Type\TypeWithClassName;

/**
 * @implements Rule<MethodCall>
 */
class SetVarArgumentTypeRule implements Rule
{
    public function getNodeType(): string
    {
        return MethodCall::class;
    }

    public function processNode(Node $node, Scope $scope): array
    {
        // 対象: $this->setVar('sum', ...)
        if (!($node->name instanceof Node\Identifier)) {
            return [];
        }

        // レシーバーの型を取得（例: $this や $store）
        $calledOnType = $scope->getType($node->var);
        [$expectedClass, $function] = [SampleController::class, 'setVar'];

        // Sample クラスでない場合はスキップ
        if (!($calledOnType instanceof TypeWithClassName) || $calledOnType->getClassName() !== $expectedClass) {
            return [];
        }

        if ($node->name->name !== $function) {
            return [];
        }

        if (count($node->args) < 2) {
            return [];
        }

        $keyArg = $node->args[0]->value;
        $valueArg = $node->args[1]->value;
        $type = $scope->getType($valueArg);
        $description = $type->describe(\PHPStan\Type\VerbosityLevel::typeOnly());

        return [
            RuleErrorBuilder::message("setVar('{$keyArg->value}', ...) の値の型は: {$description}")->build()
        ];
    }
}