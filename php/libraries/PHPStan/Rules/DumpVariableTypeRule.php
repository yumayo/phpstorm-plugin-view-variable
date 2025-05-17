<?php

declare(strict_types=1);

namespace App\Libraries\PHPStan\Rules;

use PhpParser\Node;
use PhpParser\Node\Expr\Variable;
use PHPStan\Analyser\Scope;
use PHPStan\Rules\Rule;
use PHPStan\Rules\RuleErrorBuilder;

/**
 * @implements Rule<Variable>
 */
class DumpVariableTypeRule implements Rule
{
    public function getNodeType(): string
    {
        return Variable::class;
    }

    public function processNode(Node $node, Scope $scope): array
    {
        // $varName が string のときだけ処理（$this などを除く）
        if (!is_string($node->name)) {
            return [];
        }

        $varType = $scope->getType($node);
        $typeDescription = $varType->describe(\PHPStan\Type\VerbosityLevel::typeOnly());

        $messages = [
            \PHPStan\Rules\RuleErrorBuilder::message(
                "Variable \${$node->name} has type: {$typeDescription}"
            )->build()
        ];

        return $messages;
    }
}